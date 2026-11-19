// 从 docs/FEATURES.md 生成图文版 HTML + DOCX（无第三方依赖：自带 ZIP / OOXML 写入）。
// 用法：node scripts/gen-feature-doc.mjs <version>
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';

const version = process.argv[2] || '0.0.0';
const root = path.resolve('.');
const mdPath = path.join(root, 'docs', 'FEATURES.md');
const outDir = path.join(root, 'build');
const releaseDir = path.join(root, 'release');
fs.mkdirSync(outDir, { recursive: true });
fs.mkdirSync(releaseDir, { recursive: true });

// ---------------- markdown 解析（只覆盖本文档用到的子集） ----------------
const md = fs.readFileSync(mdPath, 'utf8').replace(/\r\n/g, '\n');
const lines = md.split('\n');
const blocks = [];

const isTableRow = (l) => l.trim().startsWith('|') && l.trim().endsWith('|');
const parseInline = (text) => {
  const out = [];
  const re = /(\*\*[^*]+\*\*|`[^`]+`)/g;
  let last = 0;
  let m;
  while ((m = re.exec(text))) {
    if (m.index > last) out.push({ kind: 'text', text: text.slice(last, m.index) });
    const token = m[0];
    if (token.startsWith('**')) out.push({ kind: 'bold', text: token.slice(2, -2) });
    else out.push({ kind: 'code', text: token.slice(1, -1) });
    last = m.index + token.length;
  }
  if (last < text.length) out.push({ kind: 'text', text: text.slice(last) });
  return out;
};

let i = 0;
while (i < lines.length) {
  const line = lines[i];
  const t = line.trim();
  if (!t) { i++; continue; }
  if (t === '---') { blocks.push({ type: 'hr' }); i++; continue; }
  const heading = /^(#{1,4})\s+(.*)$/.exec(t);
  if (heading) {
    blocks.push({ type: 'heading', level: heading[1].length, text: heading[2].trim() });
    i++;
    continue;
  }
  const img = /^!\[(.*?)\]\((.*?)\)$/.exec(t);
  if (img) {
    blocks.push({ type: 'image', alt: img[1], src: img[2] });
    i++;
    continue;
  }
  if (t.startsWith('>')) {
    const quote = [];
    while (i < lines.length && lines[i].trim().startsWith('>')) {
      quote.push(lines[i].trim().replace(/^>\s?/, ''));
      i++;
    }
    blocks.push({ type: 'quote', lines: quote });
    continue;
  }
  if (t.startsWith('- ')) {
    const items = [];
    while (i < lines.length && lines[i].trim().startsWith('- ')) {
      items.push(lines[i].trim().slice(2).trim());
      i++;
    }
    blocks.push({ type: 'bullets', items });
    continue;
  }
  if (isTableRow(t)) {
    const rows = [];
    while (i < lines.length && isTableRow(lines[i].trim())) {
      const cells = lines[i].trim().slice(1, -1).split('|').map((c) => c.trim());
      const isSeparator = cells.every((c) => /^:?-{2,}:?$/.test(c));
      if (!isSeparator) rows.push(cells);
      i++;
    }
    if (rows.length) blocks.push({ type: 'table', rows });
    continue;
  }
  // 段落：合并连续非空行
  const para = [t];
  i++;
  while (i < lines.length) {
    const n = lines[i].trim();
    if (!n || n === '---' || n.startsWith('#') || n.startsWith('|') || n.startsWith('![') || n.startsWith('>') || n.startsWith('- ')) break;
    para.push(n);
    i++;
  }
  blocks.push({ type: 'paragraph', text: para.join('') });
}

// ---------------- HTML（给 Edge 打印 PDF） ----------------
const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
const inlineHtml = (text) => parseInline(text).map((p) => {
  if (p.kind === 'bold') return `<strong>${esc(p.text)}</strong>`;
  if (p.kind === 'code') return `<code>${esc(p.text)}</code>`;
  return esc(p.text);
}).join('');

let html = `<!doctype html><html lang="zh-CN"><head><meta charset="utf-8">
<title>Neko Craft Launcher ${version} 功能预览</title>
<style>
  @page { size: A4; margin: 16mm 14mm; }
  body { font-family: "Microsoft YaHei", "Segoe UI", sans-serif; color: #1d1113; font-size: 11pt; line-height: 1.65; }
  h1 { font-size: 24pt; color: #8c2438; margin: 0 0 6mm; }
  h2 { font-size: 15pt; color: #8c2438; border-bottom: 1px solid #e6c9cf; padding-bottom: 2mm; margin-top: 8mm; page-break-after: avoid; }
  h3 { font-size: 12.5pt; color: #a8324a; margin-top: 6mm; page-break-after: avoid; }
  img { width: 100%; border: 1px solid #e2cfd3; border-radius: 6px; margin: 3mm 0; page-break-inside: avoid; }
  table { width: 100%; border-collapse: collapse; margin: 3mm 0 6mm; font-size: 10pt; }
  th, td { border: 1px solid #dcc6cb; padding: 2mm 2.5mm; text-align: left; vertical-align: top; }
  th { background: #f6ecee; color: #6d1f2d; }
  code { font-family: Consolas, monospace; background: #f4eef0; padding: 0 1mm; border-radius: 3px; font-size: 9.5pt; }
  blockquote { margin: 3mm 0; padding: 2mm 4mm; border-left: 3px solid #d9a7b2; background: #faf4f5; color: #5a4348; }
  ul { margin: 2mm 0 4mm 6mm; }
  hr { border: 0; border-top: 1px solid #e6d6da; margin: 6mm 0; }
  .cover { page-break-after: always; text-align: center; padding-top: 40mm; }
  .cover .sub { color: #6a5459; font-size: 12pt; }
  .cover img { width: 78%; margin-top: 12mm; }
</style></head><body>`;

let imgs = 0;
const docsRoot = path.join(root, 'docs');
const fileUrl = (rel) => 'file:///' + path.join(docsRoot, rel).replace(/\\/g, '/');
html += `<div class="cover"><h1>Neko Craft Launcher ${version}</h1>
<div class="sub">功能预览 · 真实界面截图</div>
<img src="${fileUrl('images/preview/01-launch.png')}" alt="启动页"></div>`;

for (const b of blocks) {
  if (b.type === 'heading') {
    if (b.level === 1) continue; // 标题已用于封面
    const tag = b.level === 2 ? 'h2' : 'h3';
    html += `<${tag}>${esc(b.text)}</${tag}>`;
  } else if (b.type === 'paragraph') {
    html += `<p>${inlineHtml(b.text)}</p>`;
  } else if (b.type === 'quote') {
    html += `<blockquote>${b.lines.map((l) => inlineHtml(l)).join('<br>')}</blockquote>`;
  } else if (b.type === 'bullets') {
    html += `<ul>${b.items.map((it) => `<li>${inlineHtml(it)}</li>`).join('')}</ul>`;
  } else if (b.type === 'hr') {
    html += '<hr>';
  } else if (b.type === 'image') {
    html += `<img src="${fileUrl(b.src)}" alt="${esc(b.alt)}">`;
    imgs++;
  } else if (b.type === 'table') {
    const [head, ...rest] = b.rows;
    html += '<table><thead><tr>' + head.map((c) => `<th>${inlineHtml(c)}</th>`).join('') + '</tr></thead><tbody>';
    for (const row of rest) {
      html += '<tr>' + row.map((c) => `<td>${inlineHtml(c)}</td>`).join('') + '</tr>';
    }
    html += '</tbody></table>';
  }
}
html += '</body></html>';

const htmlPath = path.join(outDir, 'features-preview.html');
fs.writeFileSync(htmlPath, html, 'utf8');

// ---------------- DOCX（最小 OOXML） ----------------
// ZIP 写入
const crcTable = (() => {
  const table = new Int32Array(256);
  for (let n = 0; n < 256; n++) {
    let c = n;
    for (let k = 0; k < 8; k++) c = c & 1 ? 0xEDB88320 ^ (c >>> 1) : c >>> 1;
    table[n] = c;
  }
  return table;
})();
const crc32 = (buf) => {
  let c = 0 ^ -1;
  for (let i = 0; i < buf.length; i++) c = (c >>> 8) ^ crcTable[(c ^ buf[i]) & 0xFF];
  return (c ^ -1) >>> 0;
};

function zipWrite(entries) {
  const chunks = [];
  const central = [];
  let offset = 0;
  for (const e of entries) {
    const nameBuf = Buffer.from(e.name, 'utf8');
    const data = e.data;
    const comp = zlib.deflateRawSync(data, { level: 9 });
    const crc = crc32(data);
    const local = Buffer.alloc(30);
    local.writeUInt32LE(0x04034b50, 0);
    local.writeUInt16LE(20, 4);
    local.writeUInt16LE(0x0800, 6); // UTF-8 名字
    local.writeUInt16LE(8, 8);
    local.writeUInt16LE(0, 10);
    local.writeUInt16LE(0x21, 12); // 1980-01-01
    local.writeUInt32LE(crc, 14);
    local.writeUInt32LE(comp.length, 18);
    local.writeUInt32LE(data.length, 22);
    local.writeUInt16LE(nameBuf.length, 26);
    local.writeUInt16LE(0, 28);
    chunks.push(local, nameBuf, comp);

    const cd = Buffer.alloc(46);
    cd.writeUInt32LE(0x02014b50, 0);
    cd.writeUInt16LE(20, 4);
    cd.writeUInt16LE(20, 6);
    cd.writeUInt16LE(0x0800, 8);
    cd.writeUInt16LE(8, 10);
    cd.writeUInt16LE(0, 12);
    cd.writeUInt16LE(0x21, 14);
    cd.writeUInt32LE(crc, 16);
    cd.writeUInt32LE(comp.length, 20);
    cd.writeUInt32LE(data.length, 24);
    cd.writeUInt16LE(nameBuf.length, 28);
    cd.writeUInt16LE(0, 30);
    cd.writeUInt16LE(0, 32);
    cd.writeUInt16LE(0, 34);
    cd.writeUInt16LE(0, 36);
    cd.writeUInt32LE(0, 38);
    cd.writeUInt32LE(offset, 42);
    central.push(cd, nameBuf);
    offset += local.length + nameBuf.length + comp.length;
  }
  const centralBuf = Buffer.concat(central);
  const end = Buffer.alloc(22);
  end.writeUInt32LE(0x06054b50, 0);
  end.writeUInt16LE(0, 4);
  end.writeUInt16LE(0, 6);
  end.writeUInt16LE(entries.length, 8);
  end.writeUInt16LE(entries.length, 10);
  end.writeUInt32LE(centralBuf.length, 12);
  end.writeUInt32LE(offset, 16);
  end.writeUInt16LE(0, 20);
  return Buffer.concat([...chunks, centralBuf, end]);
}

// PNG 尺寸
function pngSize(buf) {
  return { w: buf.readUInt32BE(16), h: buf.readUInt32BE(20) };
}

const xmlEsc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
const EMU_PER_CM = 360000;
const CONTENT_W = Math.round(17.0 * EMU_PER_CM);

const media = [];
const mediaBySrc = new Map();
let docPrId = 1;

function runs(text, opts = {}) {
  const parts = parseInline(text);
  return parts.map((p) => {
    const rPr = [];
    if (opts.bold || p.kind === 'bold') rPr.push('<w:b/>');
    if (p.kind === 'code') rPr.push('<w:rFonts w:ascii="Consolas" w:hAnsi="Consolas"/><w:shd w:val="clear" w:color="auto" w:fill="F3EEF0"/>');
    if (opts.color) rPr.push(`<w:color w:val="${opts.color}"/>`);
    if (opts.size) rPr.push(`<w:sz w:val="${opts.size}"/><w:szCs w:val="${opts.size}"/>`);
    const rPrXml = rPr.length ? `<w:rPr>${rPr.join('')}</w:rPr>` : '';
    return `<w:r>${rPrXml}<w:t xml:space="preserve">${xmlEsc(p.text)}</w:t></w:r>`;
  }).join('');
}

function para(text, { style, bold, color, size, spacingBefore, spacingAfter, align, indent } = {}) {
  const pPr = [];
  if (style) pPr.push(`<w:pStyle w:val="${style}"/>`);
  if (spacingBefore || spacingAfter) pPr.push(`<w:spacing w:before="${spacingBefore || 0}" w:after="${spacingAfter || 0}"/>`);
  if (align) pPr.push(`<w:jc w:val="${align}"/>`);
  if (indent) pPr.push(`<w:ind w:left="${indent}"/>`);
  const pPrXml = pPr.length ? `<w:pPr>${pPr.join('')}</w:pPr>` : '';
  return `<w:p>${pPrXml}${text === '' ? '' : runs(text, { bold, color, size })}</w:p>`;
}

function imagePara(src, alt) {
  const abs = path.join(root, 'docs', src);
  if (!fs.existsSync(abs)) return para(`[缺失图片] ${src}`);
  let entry = mediaBySrc.get(src);
  if (!entry) {
    const buf = fs.readFileSync(abs);
    const { w, h } = pngSize(buf);
    entry = { name: `image${media.length + 1}.png`, buf, w, h, rid: `rId${100 + media.length}` };
    media.push(entry);
    mediaBySrc.set(src, entry);
  }
  const cx = CONTENT_W;
  const cy = Math.round(CONTENT_W * entry.h / entry.w);
  const name = entry.name;
  const rid = entry.rid;
  const id = docPrId++;
  const drawing = `<w:drawing><wp:inline distT="0" distB="0" distL="0" distR="0">`
    + `<wp:extent cx="${cx}" cy="${cy}"/><wp:docPr id="${id}" name="Picture ${id}" descr="${xmlEsc(alt)}"/>`
    + `<a:graphic><a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">`
    + `<pic:pic><pic:nvPicPr><pic:cNvPr id="${id}" name="${name}"/><pic:cNvPicPr/></pic:nvPicPr>`
    + `<pic:blipFill><a:blip r:embed="${rid}"/><a:stretch><a:fillRect/></a:stretch></pic:blipFill>`
    + `<pic:spPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="${cx}" cy="${cy}"/></a:xfrm>`
    + `<a:prstGeom prst="rect"><a:avLst/></a:prstGeom></pic:spPr></pic:pic>`
    + `</a:graphicData></a:graphic></wp:inline></w:drawing>`;
  return `<w:p><w:pPr><w:spacing w:before="120" w:after="120"/></w:pPr><w:r>${drawing}</w:r></w:p>`;
}

function tablePara(rows) {
  const cols = Math.max(...rows.map((r) => r.length));
  const widthPct = Math.floor(100 / cols);
  const borders = ['top', 'left', 'bottom', 'right', 'insideH', 'insideV']
    .map((b) => `<w:${b} w:val="single" w:sz="4" w:space="0" w:color="DCC6CB"/>`).join('');
  let xml = `<w:tbl><w:tblPr><w:tblW w:w="5000" w:type="pct"/><w:tblBorders>${borders}</w:tblBorders>`
    + `<w:tblLayout w:type="fixed"/></w:tblPr><w:tblGrid>`
    + Array.from({ length: cols }, () => `<w:gridCol w:w="${Math.floor(9360 / cols)}"/>`).join('') + '</w:tblGrid>';
  rows.forEach((row, idx) => {
    xml += '<w:tr>';
    for (let c = 0; c < cols; c++) {
      const cell = row[c] === undefined ? '' : row[c];
      const shading = idx === 0 ? '<w:shd w:val="clear" w:color="auto" w:fill="F6ECEE"/>' : '';
      xml += `<w:tc><w:tcPr><w:tcW w:w="${widthPct}" w:type="pct"/>${shading}</w:tcPr>`
        + `<w:p><w:pPr><w:spacing w:before="40" w:after="40"/></w:pPr>${runs(cell, { bold: idx === 0 })}</w:p></w:tc>`;
    }
    xml += '</w:tr>';
  });
  return xml + '</w:tbl>';
}

let body = '';
body += `<w:p><w:pPr><w:spacing w:after="120"/><w:jc w:val="center"/></w:pPr>${runs(`Neko Craft Launcher ${version}`, { bold: true, color: '8C2438', size: 52 })}</w:p>`;
body += `<w:p><w:pPr><w:jc w:val="center"/><w:spacing w:after="240"/></w:pPr>${runs('功能预览 · 真实界面截图', { color: '6A5459', size: 26 })}</w:p>`;
body += imagePara('images/preview/01-launch.png', '启动页');
body += '<w:p><w:r><w:br w:type="page"/></w:r></w:p>';

for (const b of blocks) {
  if (b.type === 'heading') {
    if (b.level === 1) continue;
    const style = b.level === 2 ? 'Heading1' : (b.level === 3 ? 'Heading2' : 'Heading3');
    body += para(b.text, { style });
  } else if (b.type === 'paragraph') {
    body += para(b.text, { spacingAfter: 120 });
  } else if (b.type === 'quote') {
    for (const l of b.lines) body += para(l, { color: '5A4348', size: 20, indent: 360 });
  } else if (b.type === 'bullets') {
    for (const it of b.items) body += para('· ' + it, { spacingAfter: 40 });
  } else if (b.type === 'hr') {
    body += '<w:p><w:pPr><w:pBdr><w:bottom w:val="single" w:sz="6" w:space="1" w:color="E6D6DA"/></w:pBdr></w:pPr></w:p>';
  } else if (b.type === 'image') {
    body += imagePara(b.src, b.alt);
  } else if (b.type === 'table') {
    body += tablePara(b.rows);
    body += '<w:p><w:pPr><w:spacing w:after="80"/></w:pPr></w:p>';
  }
}

const sectPr = '<w:sectPr><w:pgSz w:w="11906" w:h="16838"/>'
  + '<w:pgMar w:top="900" w:right="800" w:bottom="900" w:left="800" w:header="720" w:footer="720" w:gutter="0"/>'
  + '</w:sectPr>';

const documentXml = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
  + '<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"'
  + ' xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"'
  + ' xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"'
  + ' xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"'
  + ' xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">'
  + `<w:body>${body}${sectPr}</w:body></w:document>`;

const stylesXml = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
  + '<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">'
  + '<w:docDefaults><w:rPrDefault><w:rPr>'
  + '<w:rFonts w:ascii="Microsoft YaHei" w:hAnsi="Microsoft YaHei" w:eastAsia="Microsoft YaHei" w:cs="Microsoft YaHei"/>'
  + '<w:sz w:val="22"/><w:szCs w:val="22"/></w:rPr></w:rPrDefault>'
  + '<w:pPrDefault><w:pPr><w:spacing w:line="300" w:lineRule="auto"/></w:pPr></w:pPrDefault></w:docDefaults>'
  + '<w:style w:type="paragraph" w:default="1" w:styleId="Normal"><w:name w:val="Normal"/></w:style>'
  + '<w:style w:type="paragraph" w:styleId="Heading1"><w:name w:val="heading 1"/>'
  + '<w:pPr><w:spacing w:before="320" w:after="120"/><w:outlineLvl w:val="0"/></w:pPr>'
  + '<w:rPr><w:b/><w:color w:val="8C2438"/><w:sz w:val="30"/><w:szCs w:val="30"/></w:rPr></w:style>'
  + '<w:style w:type="paragraph" w:styleId="Heading2"><w:name w:val="heading 2"/>'
  + '<w:pPr><w:spacing w:before="240" w:after="80"/><w:outlineLvl w:val="1"/></w:pPr>'
  + '<w:rPr><w:b/><w:color w:val="A8324A"/><w:sz w:val="25"/><w:szCs w:val="25"/></w:rPr></w:style>'
  + '<w:style w:type="paragraph" w:styleId="Heading3"><w:name w:val="heading 3"/>'
  + '<w:pPr><w:spacing w:before="200" w:after="60"/><w:outlineLvl w:val="2"/></w:pPr>'
  + '<w:rPr><w:b/><w:sz w:val="23"/><w:szCs w:val="23"/></w:rPr></w:style>'
  + '</w:styles>';

const contentTypes = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
  + '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">'
  + '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>'
  + '<Default Extension="xml" ContentType="application/xml"/>'
  + '<Default Extension="png" ContentType="image/png"/>'
  + '<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>'
  + '<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>'
  + '</Types>';

const rootRels = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
  + '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
  + '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>'
  + '</Relationships>';

const docRels = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>'
  + '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">'
  + '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>'
  + media.map((m) => `<Relationship Id="${m.rid}" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/${m.name}"/>`).join('')
  + '</Relationships>';

const entries = [
  { name: '[Content_Types].xml', data: Buffer.from(contentTypes, 'utf8') },
  { name: '_rels/.rels', data: Buffer.from(rootRels, 'utf8') },
  { name: 'word/document.xml', data: Buffer.from(documentXml, 'utf8') },
  { name: 'word/styles.xml', data: Buffer.from(stylesXml, 'utf8') },
  { name: 'word/_rels/document.xml.rels', data: Buffer.from(docRels, 'utf8') },
  ...media.map((m) => ({ name: `word/media/${m.name}`, data: m.buf })),
];

// relId 计数与实际写入的媒体一一对应（先收集再写 rels，顺序一致）
const docxPath = path.join(releaseDir, `NekoCraftLauncher-${version}-Features.docx`);
fs.writeFileSync(docxPath, zipWrite(entries));

console.log(`html=${htmlPath} (images=${imgs})`);
console.log(`docx=${docxPath} (media=${media.length})`);
