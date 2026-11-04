package com.example.demo;

import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;

/** 像素级皮肤预览：Canvas 软件光栅化，逐像素最近邻采样。 */
public final class Skin3DPreview extends StackPane {

    private static final int W = 420, H = 560;
    private final Canvas canvas = new Canvas(W, H);
    private final WritableImage buffer = new WritableImage(W, H);
    private final int[] pixels = new int[W * H];
    private final double[] depth = new double[W * H];
    private Image skin, cape;
    private boolean slim;
    private String background = "浅绿";
    private String pose = "idle";
    private double animTime;
    private double yaw = -18, pitch = 8, zoom = 1.0;
    private double lastX, lastY;

    public Skin3DPreview() {
        setPrefSize(W, H);
        setMinSize(W, H);
        setMaxSize(W, H);
        setStyle("-fx-background-radius: 16; -fx-border-color: #cfe5c8; -fx-border-radius: 16;");
        getChildren().add(canvas);
        javafx.scene.control.Label hint = new javafx.scene.control.Label("拖动旋转 · 滚轮缩放");
        hint.setStyle("-fx-text-fill: rgba(90,130,84,0.65); -fx-font-size: 9px;"
                + " -fx-background-color: rgba(255,255,255,0.55); -fx-background-radius: 8; -fx-padding: 2 7;");
        StackPane.setAlignment(hint, Pos.BOTTOM_CENTER);
        StackPane.setMargin(hint, new javafx.geometry.Insets(0, 0, 10, 0));
        getChildren().add(hint);
        setOnMousePressed(e -> { lastX = e.getSceneX(); lastY = e.getSceneY(); });
        setOnMouseDragged(e -> {
            yaw += (e.getSceneX() - lastX) * 0.6;
            pitch = Math.max(-30, Math.min(30, pitch + (e.getSceneY() - lastY) * 0.35));
            lastX = e.getSceneX(); lastY = e.getSceneY();
            draw();
        });
        setOnScroll(e -> {
            zoom = Math.max(0.7, Math.min(1.8, zoom * (e.getDeltaY() > 0 ? 1.08 : 0.92)));
            draw();
        });
        new javafx.animation.AnimationTimer() {
            private long last;
            @Override public void handle(long now) {
                if (last == 0) { last = now; return; }
                if (now - last < 50_000_000L) return;
                last = now;
                animTime += 0.05;
                draw();
            }
        }.start();
        draw();
    }

    public void setImages(Image skinImage, Image capeImage) {
        this.skin = skinImage;
        this.cape = capeImage;
        draw();
    }

    public void setSlim(boolean slim) { this.slim = slim; draw(); }
    public void setWalking(boolean walking) { this.pose = walking ? "walk" : "idle"; }
    public void setPose(String pose) { this.pose = pose == null ? "idle" : pose; }
    public void setBackgroundMode(String mode) { this.background = mode == null ? "浅绿" : mode; draw(); }

    private record Part(double w, double h, double d, double x, double y, double z,
                        int[] uv, int[] overlayUv) {}
    private record Face(int[] tex, int tw, int th, double[] px, double[] py, double[] pz) {}

    private void draw() {
        int bg = switch (background) {
            case "夜景" -> 0xff111827;
            case "粉色" -> 0xfffce7f3;
            case "天空" -> 0xffdbeafe;
            case "透明" -> 0x00000000;
            default -> 0xffeef8e9;
        };
        java.util.Arrays.fill(pixels, bg);
        java.util.Arrays.fill(depth, Double.MAX_VALUE);
        if (skin != null) {
            List<Part> parts = new ArrayList<>();
            parts.add(new Part(80, 120, 40, 0, 10, 0,
                    new int[]{20,20, 28,20, 16,20, 32,20, 20,16, 28,16},
                    new int[]{20,36, 28,36, 16,36, 32,36, 20,32, 28,32}));
            parts.add(new Part(80, 80, 80, 0, -90, 0,
                    new int[]{8,8, 24,8, 0,8, 16,8, 8,0, 16,0},
                    new int[]{40,8, 56,8, 32,8, 48,8, 40,0, 48,0}));
            double arm = slim ? 30 : 40;
            parts.add(new Part(arm, 120, 40, -60, 10, 0,
                    new int[]{36,52, 44,52, 32,52, 40,52, 36,48, 40,48},
                    new int[]{52,52, 60,52, 48,52, 56,52, 52,48, 56,48}));
            parts.add(new Part(arm, 120, 40, 60, 10, 0,
                    new int[]{44,20, 52,20, 40,20, 48,20, 44,16, 48,16},
                    new int[]{44,36, 52,36, 40,36, 48,36, 44,32, 48,32}));
            parts.add(new Part(40, 120, 40, -20, 130, 0,
                    new int[]{20,52, 28,52, 16,52, 24,52, 20,48, 24,48},
                    new int[]{4,52, 12,52, 0,52, 8,52, 4,48, 8,48}));
            parts.add(new Part(40, 120, 40, 20, 130, 0,
                    new int[]{4,20, 12,20, 0,20, 8,20, 4,16, 8,16},
                    new int[]{4,36, 12,36, 0,36, 8,36, 4,32, 8,32}));
            double swing = switch (pose) {
                case "walk" -> Math.sin(animTime * 6) * 32;
                case "run" -> Math.sin(animTime * 9) * 55;
                case "wave" -> Math.sin(animTime * 6) * 18;
                default -> Math.sin(animTime * 1.5) * 3;
            };
            for (int i = 0; i < parts.size(); i++) {
                Part part = parts.get(i);
                double angle = 0, pivot = 0;
                if (i == 2) { angle = swing; pivot = -50; }
                if (i == 3) { angle = -swing; pivot = -50; }
                if (i == 4) { angle = -swing; pivot = 70; }
                if (i == 5) { angle = swing; pivot = 70; }
                addBox(part, part.uv(), 1.0, angle, pivot);
                addBox(part, part.overlayUv(), 1.025, angle, pivot);
            }
        }
        buffer.getPixelWriter().setPixels(0, 0, W, H, PixelFormat.getIntArgbInstance(), pixels, 0, W);
        GraphicsContext g = canvas.getGraphicsContext2D();
        g.setImageSmoothing(false);
        g.clearRect(0, 0, W, H);
        g.drawImage(buffer, 0, 0);
    }

    private void addBox(Part p, int[] uv, double expand, double angleX, double pivotY) {
        double w = p.w * expand, h = p.h * expand, d = p.d * expand;
        double[][] v = {
                {-w/2,-h/2,-d/2}, {w/2,-h/2,-d/2}, {w/2,h/2,-d/2}, {-w/2,h/2,-d/2},
                {w/2,-h/2,d/2}, {-w/2,-h/2,d/2}, {-w/2,h/2,d/2}, {w/2,h/2,d/2}
        };
        int[][] f = {{0,1,2,3}, {4,5,6,7}, {1,4,7,2}, {5,0,3,6}, {5,4,1,0}, {3,2,7,6}};
        for (int i = 0; i < 6; i++) {
            int[] idx = f[i];
            double[] xs = new double[4], ys = new double[4], zs = new double[4];
            for (int j = 0; j < 4; j++) {
                double wy = p.y + v[idx[j]][1], wz = p.z + v[idx[j]][2];
                if (angleX != 0) {
                    double rad = Math.toRadians(angleX);
                    double ly = wy - pivotY, lz = wz;
                    wy = pivotY + ly * Math.cos(rad) - lz * Math.sin(rad);
                    wz = ly * Math.sin(rad) + lz * Math.cos(rad);
                }
                double[] q = project(p.x + v[idx[j]][0], wy, wz);
                xs[j] = q[0]; ys[j] = q[1]; zs[j] = q[2];
            }
            int texW = (int) Math.round(((i < 2) ? w : (i < 4) ? d : w) / 10.0);
            int texH = (int) Math.round(((i < 2) ? h : (i < 4) ? h : d) / 10.0);
            int[] tex = cropPixels(uv[i*2], uv[i*2+1], texW, texH);
            if (tex != null) raster(new Face(tex, texW, texH, xs, ys, zs));
        }
    }

    private double[] project(double x, double y, double z) {
        double cy = Math.cos(Math.toRadians(yaw)), sy = Math.sin(Math.toRadians(yaw));
        double cp = Math.cos(Math.toRadians(pitch)), sp = Math.sin(Math.toRadians(pitch));
        double x1 = x * cy + z * sy;
        double z1 = -x * sy + z * cy;
        double y1 = y * cp - z1 * sp;
        double z2 = y * sp + z1 * cp;
        double d = 1000 + z2;
        double s = 760.0 / d * zoom;
        return new double[]{W / 2.0 + x1 * s, H / 2.0 + y1 * s, d};
    }

    private void raster(Face face) {
        tri(face, 0, 1, 2, 0, 0, face.tw, 0, face.tw, face.th);
        tri(face, 0, 2, 3, 0, 0, face.tw, face.th, 0, face.th);
    }

    private void tri(Face face, int ia, int ib, int ic,
                     double ua, double va, double ub, double vb, double uc, double vc) {
        double ax = face.px[ia], ay = face.py[ia], az = face.pz[ia];
        double bx = face.px[ib], by = face.py[ib], bz = face.pz[ib];
        double cx = face.px[ic], cy = face.py[ic], cz = face.pz[ic];
        double area = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
        if (Math.abs(area) < 1e-6) return;
        int minX = Math.max(0, (int) Math.floor(Math.min(ax, Math.min(bx, cx))));
        int maxX = Math.min(W - 1, (int) Math.ceil(Math.max(ax, Math.max(bx, cx))));
        int minY = Math.max(0, (int) Math.floor(Math.min(ay, Math.min(by, cy))));
        int maxY = Math.min(H - 1, (int) Math.ceil(Math.max(ay, Math.max(by, cy))));
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                double px = x + 0.5, py = y + 0.5;
                double w0 = ((bx - px) * (cy - py) - (by - py) * (cx - px)) / area;
                double w1 = ((cx - px) * (ay - py) - (cy - py) * (ax - px)) / area;
                double w2 = 1 - w0 - w1;
                if (w0 < 0 || w1 < 0 || w2 < 0) continue;
                double z = w0 * az + w1 * bz + w2 * cz;
                int idx = y * W + x;
                if (z >= depth[idx]) continue;
                int u = (int) (w0 * ua + w1 * ub + w2 * uc);
                int v = (int) (w0 * va + w1 * vb + w2 * vc);
                u = Math.max(0, Math.min(face.tw - 1, u));
                v = Math.max(0, Math.min(face.th - 1, v));
                int src = face.tex[v * face.tw + u];
                int a = (src >>> 24) & 0xff;
                if (a == 0) continue;
                pixels[idx] = blend(src, pixels[idx]);
                if (a >= 250) depth[idx] = z;
            }
        }
    }

    private int[] cropPixels(int u, int v, int w, int h) {
        if (skin == null) return null;
        int maxW = (int) skin.getWidth(), maxH = (int) skin.getHeight();
        u = Math.max(0, Math.min(u, maxW - 1));
        v = Math.max(0, Math.min(v, maxH - 1));
        w = Math.max(1, Math.min(w, maxW - u));
        h = Math.max(1, Math.min(h, maxH - v));
        int[] out = new int[w * h];
        skin.getPixelReader().getPixels(u, v, w, h, PixelFormat.getIntArgbInstance(), out, 0, w);
        return out;
    }

    private int blend(int src, int dst) {
        int sa = (src >>> 24) & 0xff;
        if (sa >= 255) return src;
        int sr = (src >> 16) & 0xff, sg = (src >> 8) & 0xff, sb = src & 0xff;
        int da = (dst >>> 24) & 0xff, dr = (dst >> 16) & 0xff, dg = (dst >> 8) & 0xff, db = dst & 0xff;
        int outA = sa + da * (255 - sa) / 255;
        if (outA == 0) return 0;
        int outR = (sr * sa + dr * da * (255 - sa) / 255) / outA;
        int outG = (sg * sa + dg * da * (255 - sa) / 255) / outA;
        int outB = (sb * sa + db * da * (255 - sa) / 255) / outA;
        return (outA << 24) | (outR << 16) | (outG << 8) | outB;
    }
}
