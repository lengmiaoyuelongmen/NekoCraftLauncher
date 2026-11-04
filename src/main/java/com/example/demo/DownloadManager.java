package com.example.demo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/** 下载引擎：断点续传、SHA1 校验、镜像回退、并发下载、进度回调。 启动器的资源/依赖/客户端 jar 全部走这里。 */
public final class DownloadManager {

  private static final HttpClient CLIENT =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(8))
          .followRedirects(HttpClient.Redirect.NORMAL)
          .build();

  /** 单个请求的超时：卡住的连接要尽快放弃，否则会拖死整个启动流程。 */
  private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(90);

  private DownloadManager() {}

  /** 一个待下载文件。sha1/size 允许为 null/-1（未知就不校验）。 */
  public static final class Task {
    public final String url;
    public final File target;
    public final String sha1;
    public final long size;

    public Task(String url, File target, String sha1, long size) {
      this.url = url;
      this.target = target;
      this.sha1 = sha1;
      this.size = size;
    }
  }

  public interface Progress {
    void onProgress(int done, int total, long bytes);
  }

  /** 全局下载状态（给启动页底部的下载面板用）。 */
  public record Status(int done, int total, long bytes, long totalBytes, String file, long speedBps) {}

  /** 本批任务计划下载的总字节数（未知时为 0）。 */
  /** 本批「已知大小」任务的总字节。 */
  private static volatile long batchPlannedBytes;
  /** 本批「大小未知」任务后来探明的大小（避免进度条虚标/超过 100%）。 */
  private static final java.util.concurrent.atomic.AtomicLong batchDiscoveredBytes =
      new java.util.concurrent.atomic.AtomicLong();

  private static volatile Status status = new Status(0, 0, 0, 0, "", 0);
  private static final java.util.List<java.util.function.Consumer<Status>> statusListeners =
      new java.util.concurrent.CopyOnWriteArrayList<>();
  private static volatile String currentFile = "";
  private static volatile int currentDone = 0;
  private static volatile int currentTotal = 0;
  private static final java.util.concurrent.atomic.AtomicLong transferredBytes =
      new java.util.concurrent.atomic.AtomicLong();
  private static final AtomicInteger activeDownloads = new AtomicInteger();
  private static volatile boolean cancelRequested;

  public static void cancelAll() {
    cancelRequested = true;
  }

  private static void checkCancelled() {
    if (cancelRequested) throw new java.util.concurrent.CancellationException("download cancelled");
  }

  public static Status status() {
    return status;
  }

  public static void removeStatusListener(java.util.function.Consumer<Status> listener) {
    statusListeners.remove(listener);
  }

  public static void addStatusListener(java.util.function.Consumer<Status> listener) {
    statusListeners.add(listener);
  }

  /** 进度通知节流：几千个文件逐个回调会把 FX 线程刷爆（启动慢的主因之一）。 */
  private static final long PUBLISH_INTERVAL_MS = 120L;
  private static volatile long lastPublishMs;

  private static void publishStatus(int done, int total, long bytes, String file, long speedBps) {
    long totalBytes = batchPlannedBytes + batchDiscoveredBytes.get();
    Status next = new Status(done, total, bytes, totalBytes, file, speedBps);
    status = next;
    long now = System.currentTimeMillis();
    boolean finished = total > 0 && done >= total;
    if (!finished && now - lastPublishMs < PUBLISH_INTERVAL_MS) {
      return;
    }
    lastPublishMs = now;
    for (java.util.function.Consumer<Status> listener : statusListeners) {
      try {
        listener.accept(next);
      } catch (Exception ignored) {
        // 单个监听者出错不影响下载
      }
    }
  }

  public static final class Stats {
    public int downloaded;
    public int skipped;
    public int failed;
    public int repaired;
    public long bytes;
    public final java.util.List<String> failures =
        java.util.Collections.synchronizedList(new ArrayList<>());
  }

  /** 并发下载一批任务；已存在且大小一致的直接跳过。 */
  public static Stats downloadAll(List<Task> tasks, int threads, Progress progress) {
    return downloadAll(tasks, threads, progress, "auto", 0);
  }

  /**
   * @param source auto / official / mirror
   * @param speedLimitKb 每个任务的速度上限（KB/s），0 表示不限
   */
  public static Stats downloadAll(
      List<Task> tasks, int threads, Progress progress, String source, int speedLimitKb) {
    currentSource = source == null ? "auto" : source;
    currentSpeedLimitBytes = Math.max(0, speedLimitKb) * 1024L;
    Stats stats = new Stats();
    List<Task> pending = new ArrayList<>();
    for (Task task : tasks) {
      if (isUpToDate(task)) {
        stats.skipped++;
      } else {
        pending.add(task);
      }
    }
    int total = pending.size();
    long startedAt = System.currentTimeMillis();
    currentFile = "";
    currentTotal = total;
    currentDone = 0;
    transferredBytes.set(0);
    publishStatus(0, total, 0, "", 0);
    if (total == 0) {
      if (progress != null) progress.onProgress(1, 1, 0);
      return stats;
    }

    // 记下本批计划总字节，供界面算「预计剩余」；嵌套调用（ranged 回退）不能覆盖外层统计
    if (activeDownloads.get() <= 0) {
      long planned = 0L;
      for (Task task : pending) {
        if (task.size > 0) planned += task.size;
      }
      batchPlannedBytes = planned;
      batchDiscoveredBytes.set(0L);
    }

    // 每批开始都清掉上一批的取消标志，否则上一批的取消会毒到这一批
    cancelRequested = false;
    activeDownloads.addAndGet(total);
    AtomicInteger done = new AtomicInteger();
    AtomicLong bytes = new AtomicLong();
    AtomicLong lastReport = new AtomicLong(0);
    int adaptiveThreads = total >= 32 ? Math.max(threads, 16) : Math.max(1, threads);
    ExecutorService pool = Executors.newFixedThreadPool(adaptiveThreads);
    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (Task task : pending) {
      futures.add(
          CompletableFuture.runAsync(
              () -> {
                currentFile = task.target.getName();
                publishStatus(done.get(), total, transferredBytes.get(), currentFile, 0);
                boolean ok;
                if (task.size >= 1024L * 1024 && threads >= 1 && total <= 8) {
                  // 实测 8 连接比 4 连接快约 15%~20%，再多反而变慢
                  int parts = task.size >= 4L * 1024 * 1024 ? 8 : 4;
                  ok = downloadRanged(task, parts, null).downloaded > 0;
                } else {
                  ok = downloadWithRetry(task);
                }
                synchronized (stats) {
                  if (ok) {
                    stats.downloaded++;
                    if (task.size > 0) stats.bytes += task.size;
                  } else {
                    stats.failed++;
                    stats.failures.add(task.url);
                  }
                }
                int finished = done.incrementAndGet();
                bytes.addAndGet(Math.max(0, task.size));
                long now = System.currentTimeMillis();
                currentDone = finished;
                if (progress != null && (finished == total || now - lastReport.get() > 120)) {
                  lastReport.set(now);
                  progress.onProgress(finished, total, bytes.get());
                }
                long elapsed = Math.max(1, System.currentTimeMillis() - startedAt);
                publishStatus(
                    finished,
                    total,
                    transferredBytes.get(),
                    currentFile,
                    transferredBytes.get() * 1000 / elapsed);
              },
              pool));
    }
    // 任一任务抛异常都不能让计数器/线程池漏掉，否则 awaitIdle 永远等不到
    try {
      CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
    } finally {
      activeDownloads.addAndGet(-total);
      pool.shutdownNow();
      if (activeDownloads.get() <= 0) {
        batchPlannedBytes = 0L;
        batchDiscoveredBytes.set(0L);
      }
    }
    publishStatus(total, total, transferredBytes.get(), "", 0);
    return stats;
  }

  /** 已存在且校验通过 → 认为完整。size 未知时至少验证 zip 结构，避免把截断文件当成完整。 */
  private static boolean isUpToDate(Task task) {
    File file = task.target;
    if (!file.isFile() || file.length() == 0) return false;
    if (task.size > 0 && file.length() != task.size) return false;
    String name = file.getName().toLowerCase(java.util.Locale.ROOT);
    boolean archive =
        name.endsWith(".jar") || name.endsWith(".zip") || name.endsWith(".mrpack");
    if (!archive) {
      return task.size > 0;
    }
    // 压缩包：能被 ZipFile 正常打开才认为没坏（截断的 zip 会在打开/读条目时报错）
    try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(file)) {
      return zip.size() > 0;
    } catch (Exception broken) {
      return false;
    }
  }

  private static boolean downloadWithRetry(Task task) {
    for (int attempt = 1; attempt <= 2; attempt++) {
      if (downloadOnce(task)) {
        return true;
      }
    }
    return false;
  }

  private static boolean downloadOnce(Task task) {
    File target = task.target;
    if (aria2Enabled
        && Aria2Downloader.available()
        && (task.size <= 0 || task.size >= 512L * 1024)) {
      // 大文件优先交给 aria2（多连接分段），失败再回退内置 HTTP 下载
      boolean done =
          Aria2Downloader.download(
              target,
              task.url,
              task.sha1,
              task.size,
              new Aria2Downloader.Progress() {
                private boolean credited;

                @Override
                public void update(long downloaded, long total, long speedBps) {
                  if (!credited && task.size <= 0 && total > 0) {
                    credited = true;
                    batchDiscoveredBytes.addAndGet(total);
                  }
                  publishStatus(
                      currentDone, currentTotal, downloaded, target.getName(), speedBps);
                }
              },
              () -> cancelRequested);
      if (done) {
        report("⚡ 已用 aria2 引擎下载: " + target.getName());
        return true;
      }
    }
    File parent = target.getParentFile();
    if (parent != null && !parent.exists() && !parent.mkdirs()) return false;

    File part = new File(target.getAbsolutePath() + ".part");
    long existing = part.isFile() ? part.length() : 0;

    for (String url : candidates(task.url)) {
      boolean isMirror = !url.equals(task.url);
      try {
        HttpRequest.Builder builder =
            HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "MCLauncher/1.0")
                .GET();
        if (existing > 0) {
          builder.header("Range", "bytes=" + existing + "-");
        }
        HttpResponse<InputStream> response =
            CLIENT.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());

        if (response.statusCode() == 206) {
          // 续传
          try (InputStream in = response.body();
              FileOutputStream out = new FileOutputStream(part, true)) {
            copy(in, out, target.getName());
          }
        } else if (response.statusCode() == 416 && existing > 0) {
          // 续传位置无效（常见于残留的坏 .part），从头再来一次
          part.delete();
          existing = 0;
          continue;
        } else if (response.statusCode() == 200) {
          String contentType = response.headers().firstValue("Content-Type").orElse("");
          if (contentType.contains("text/html") && !url.endsWith(".json")) continue;
          if (task.size <= 0) {
            long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1L);
            if (contentLength > 0) {
              batchDiscoveredBytes.addAndGet(contentLength);
            }
          }
          try (InputStream in = response.body();
              FileOutputStream out = new FileOutputStream(part, false)) {
            copy(in, out, target.getName());
          }
        } else {
          // 读一点 body 判断是不是 CDN 源站错误（Modrinth 偶发 400 OnDemandMigrationSourceStatus）
          try (InputStream errBody = response.body()) {
            String body = new String(errBody.readNBytes(256), java.nio.charset.StandardCharsets.UTF_8);
            if (body.contains("OnDemandMigrationSourceStatus")) {
              report("⚠ 该文件的 CDN 源站暂时不可用（Modrinth 返回 "
                  + response.statusCode() + "），正在尝试镜像源…");
            } else if (response.statusCode() >= 400) {
              report("⚠ 下载源返回 HTTP " + response.statusCode() + "，尝试下一个源");
            }
          } catch (Exception ignored) {
            // 读错误体失败就按普通失败处理
          }
          continue;
        }

        if (part.length() == 0) continue;

        if (task.sha1 != null && !task.sha1.isBlank()) {
          String actual = sha1(part);
          if (!actual.equalsIgnoreCase(task.sha1)) {
            // 校验失败：删掉重下一次
            part.delete();
            if (existing > 0) {
              existing = 0;
              continue;
            }
            continue;
          }
        }
        Files.move(part.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        return true;
      } catch (java.util.concurrent.CancellationException cancelled) {
        // 用户取消不是「下载失败」，直接向上抛，别计入 failed
        throw cancelled;
      } catch (java.io.IOException networkError) {
        // 只有网络/IO 类失败才把镜像标记为不可用
        if (isMirror) mirrorDisabled = true;
      } catch (Exception other) {
        // 其它异常（磁盘、权限）不要连累镜像，也不要冒充成功
        if (isMirror && other instanceof java.net.SocketException) mirrorDisabled = true;
      }
    }
    // 都失败时保留 .part，下次可以续传
    return false;
  }

  /** 一旦某个镜像超时/失败，本轮就不再尝试它（避免每个文件都等一次超时）。 */
  private static volatile boolean mirrorDisabled = false;

  /** 是否优先用 aria2（Motrix 自带引擎）下载；不可用时自动回退内置下载器。 */
  private static volatile boolean aria2Enabled = true;

  public static void setAria2Enabled(boolean enabled) {
    aria2Enabled = enabled;
  }

  public static boolean isAria2Enabled() {
    return aria2Enabled && Aria2Downloader.available();
  }

  /** 下载过程中的一次性提示（源站错误等），由主类接管写日志。 */
  private static volatile java.util.function.Consumer<String> messageSink;

  public static void setMessageSink(java.util.function.Consumer<String> sink) {
    messageSink = sink;
  }

  private static void report(String message) {
    java.util.function.Consumer<String> sink = messageSink;
    if (sink != null) {
      sink.accept(message);
    }
  }

  private static volatile String currentSource = "auto";
  private static volatile long currentSpeedLimitBytes = 0;

  /** 按限速拷贝数据（0 表示全速）。 */
  private static void copy(InputStream in, OutputStream out) throws Exception {
    copy(in, out, "");
  }

  private static void copy(InputStream in, OutputStream out, String fileName) throws Exception {
    byte[] buffer = new byte[16384];
    long windowStart = System.currentTimeMillis();
    long windowBytes = 0;
    long totalBytes = 0;
    long lastPublish = 0;
    int read;
    while ((read = in.read(buffer)) != -1) {
      totalBytes += read;
      // 源太慢就主动断开换下一个源，避免卡在几十 KB/s 上干等
      long alive = System.currentTimeMillis() - windowStart;
      if (alive > 8000L && totalBytes * 1000L / alive < 20L * 1024L) {
        throw new java.io.IOException("源速度过低（" + (totalBytes * 1000L / alive / 1024L) + " KB/s），切换下一个源");
      }
      checkCancelled();
      out.write(buffer, 0, read);
      windowBytes += read;
      transferredBytes.addAndGet(read);
      long now = System.currentTimeMillis();
      if (now - lastPublish > 200) {
        lastPublish = now;
        long elapsed = Math.max(1, now - windowStart);
        long speed = windowBytes * 1000 / elapsed;
        publishStatus(currentDone, currentTotal, transferredBytes.get(), fileName, speed);
      }
      long limit = currentSpeedLimitBytes;
      if (limit > 0) {
        long elapsed = System.currentTimeMillis() - windowStart;
        long expected = windowBytes * 1000 / limit;
        if (elapsed < expected) {
          Thread.sleep(Math.min(200, expected - elapsed));
        }
        if (elapsed > 2000) {
          windowStart = System.currentTimeMillis();
          windowBytes = 0;
        }
      }
    }
  }

  /** 大文件分段并发下载：多连接 Range 聚合，失败自动回退单连接。 */
  public static Stats downloadRanged(Task task, int connections, Progress progress) {
    long total = task.size > 0 ? task.size : headSize(task.url);
    if (activeDownloads.get() <= 0) {
      batchPlannedBytes = total > 0 ? total : 0L;
      batchDiscoveredBytes.set(0L);
    }
    if (total < 2L * 1024 * 1024 || connections <= 1) {
      return downloadAll(List.of(task), 1, progress);
    }
    activeDownloads.incrementAndGet();
    try {
      int parts = Math.max(2, Math.min(8, connections));
      long chunk = (total + parts - 1) / parts;
      java.util.concurrent.ExecutorService pool =
          java.util.concurrent.Executors.newFixedThreadPool(parts);
      java.util.concurrent.atomic.AtomicLong done = new java.util.concurrent.atomic.AtomicLong();
      java.util.concurrent.atomic.AtomicInteger failed =
          new java.util.concurrent.atomic.AtomicInteger();
      List<File> partFiles = new ArrayList<>();
      List<CompletableFuture<Void>> futures = new ArrayList<>();
      List<String> segmentUrls = candidates(task.url);
      long started = System.currentTimeMillis();
      for (int i = 0; i < parts; i++) {
        long start = i * chunk;
        long end = Math.min(total - 1, start + chunk - 1);
        if (start > end) break;
        File part = new File(task.target.getAbsolutePath() + ".part" + i);
        partFiles.add(part);
        final int index = i;
        futures.add(
            CompletableFuture.runAsync(
                () -> {
                  for (int attempt = 0; attempt < 2; attempt++) {
                    String segmentUrl = segmentUrls.get((index + attempt) % segmentUrls.size());
                    try {
                      HttpRequest req =
                          HttpRequest.newBuilder(URI.create(segmentUrl))
                              .timeout(REQUEST_TIMEOUT)
                              .header("Range", "bytes=" + start + "-" + end)
                              .GET()
                              .build();
                      HttpResponse<InputStream> resp =
                          CLIENT.send(req, HttpResponse.BodyHandlers.ofInputStream());
                      if (resp.statusCode() != 206)
                        throw new RuntimeException("HTTP " + resp.statusCode());
                      try (InputStream in = resp.body();
                          OutputStream out = Files.newOutputStream(part.toPath())) {
                        byte[] buffer = new byte[65536];
                        int read;
                        while ((read = in.read(buffer)) != -1) {
                          checkCancelled();
                          out.write(buffer, 0, read);
                          long nowDone = done.addAndGet(read);
                          long elapsed = Math.max(1, System.currentTimeMillis() - started);
                          publishStatus(
                              currentDone,
                              currentTotal,
                              nowDone,
                              task.target.getName(),
                              nowDone * 1000 / elapsed);
                          if (progress != null && (nowDone & 0x3FFFF) < 4096) {
                            progress.onProgress(
                                (int) Math.min(100, nowDone * 100 / total), 100, nowDone);
                          }
                        }
                      }
                      return;
                    } catch (Exception ignored) {
                      part.delete();
                    }
                  }
                  failed.incrementAndGet();
                },
                pool));
      }
      try {
        CompletableFuture.allOf(futures.toArray(new CompletableFuture<?>[0])).join();
      } finally {
        pool.shutdown();
      }
      Stats stats = new Stats();
      if (failed.get() == 0) {
        try {
          try (OutputStream out = Files.newOutputStream(task.target.toPath())) {
            for (File part : partFiles) {
              if (!part.isFile()) throw new RuntimeException("分段缺失");
              Files.copy(part.toPath(), out);
            }
          }
          if (task.sha1 != null
              && !task.sha1.isBlank()
              && !task.sha1.equalsIgnoreCase(sha1(task.target))) {
            throw new RuntimeException("SHA1 校验失败");
          }
          // 合并成功：删掉所有分段临时文件，避免每次大文件下载都残留 .part0..N
          for (File part : partFiles) {
            part.delete();
          }
          stats.downloaded = 1;
          stats.bytes = total;
          publishStatus(1, 1, total, task.target.getName(), 0);
          return stats;
        } catch (Exception ignored) {
          task.target.delete();
        }
      }
      for (File part : partFiles) part.delete();
      // 分段路径整体失败：走单连接重试，失败也不把脏文件留在用户目录
      return downloadAll(List.of(task), 1, progress);
    } finally {
      if (activeDownloads.decrementAndGet() == 0) {
        batchPlannedBytes = 0L;
        batchDiscoveredBytes.set(0L);
        publishStatus(0, 0, 0, "", 0);
      }
    }
  }

  private static long headSize(String url) {
    try {
      HttpRequest req =
          HttpRequest.newBuilder(URI.create(url))
              .timeout(Duration.ofSeconds(20))
              .header("Range", "bytes=0-0")
              .GET()
              .build();
      HttpResponse<InputStream> resp = CLIENT.send(req, HttpResponse.BodyHandlers.ofInputStream());
      InputStream body = resp.body();
      try (body) {
        String range = resp.headers().firstValue("Content-Range").orElse("");
        int slash = range.lastIndexOf('/');
        if (slash >= 0) return Long.parseLong(range.substring(slash + 1));
      }
    } catch (Exception ignored) {
    }
    return -1;
  }

  /** 等待所有正在进行/排队的下载结束，供启动游戏前做全局闸门。 */
  public static boolean awaitIdle(long timeoutMs) {
    long deadline = System.currentTimeMillis() + Math.max(1000, timeoutMs);
    while (activeDownloads.get() > 0 && System.currentTimeMillis() < deadline) {
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return false;
      }
    }
    return activeDownloads.get() == 0;
  }

  public static int activeCount() {
    return activeDownloads.get();
  }

  private static List<String> candidates(String officialUrl) {
    String mirror =
        officialUrl
            .replace("libraries.minecraft.net", "bmclapi2.bangbang93.com/maven")
            .replace("piston-data.mojang.com", "bmclapi2.bangbang93.com")
            .replace("launchermeta.mojang.com", "bmclapi2.bangbang93.com")
            .replace("resources.download.minecraft.net", "bmclapi2.bangbang93.com/assets")
            // Modrinth 文件走 MCIM 国内镜像（官方文档：/data/{project}/versions/{version}/{file}）
            .replace("https://cdn.modrinth.com/", "https://mod.mcimirror.top/");
    List<String> list = new ArrayList<>();
    boolean useMirror =
        !mirror.equals(officialUrl) && !mirrorDisabled && !"official".equals(currentSource);
    boolean useOfficial = !"mirror".equals(currentSource);
    if (useMirror) list.add(mirror);
    if (useOfficial || list.isEmpty()) list.add(officialUrl);
    // 旧版本库经常在 BMCLAPI/官方源缺失，补 Maven Central
    if (officialUrl.contains("libraries.minecraft.net/")) {
      String central =
          officialUrl.replace(
              "https://libraries.minecraft.net/", "https://repo1.maven.org/maven2/");
      if (!list.contains(central)) list.add(central);
    }
    return list;
  }

  public static String sha1(File file) {
    try (InputStream in = Files.newInputStream(file.toPath())) {
      MessageDigest digest = MessageDigest.getInstance("SHA-1");
      byte[] buffer = new byte[65536];
      int read;
      while ((read = in.read(buffer)) != -1) {
        digest.update(buffer, 0, read);
      }
      StringBuilder sb = new StringBuilder();
      for (byte b : digest.digest()) {
        sb.append(String.format(Locale.ROOT, "%02x", b));
      }
      return sb.toString();
    } catch (Exception e) {
      return "";
    }
  }
}
