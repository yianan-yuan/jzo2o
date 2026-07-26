const toBytes = (chunk) => {
  if (chunk instanceof Uint8Array) return chunk;
  if (ArrayBuffer.isView(chunk)) return new Uint8Array(chunk.buffer, chunk.byteOffset, chunk.byteLength);
  return new Uint8Array(chunk || new ArrayBuffer(0));
};

const concatBytes = (left, right) => {
  const result = new Uint8Array(left.length + right.length);
  result.set(left);
  result.set(right, left.length);
  return result;
};

const codePointToString = (codePoint) => {
  if (codePoint <= 0xffff) return String.fromCharCode(codePoint);
  const value = codePoint - 0x10000;
  return String.fromCharCode(0xd800 + (value >> 10), 0xdc00 + (value & 0x3ff));
};

const createFallbackUtf8Decoder = () => {
  let pending = new Uint8Array(0);
  return {
    decode(chunk, options = {}) {
      const bytes = concatBytes(pending, toBytes(chunk));
      const stream = options.stream === true;
      let text = '';
      let offset = 0;
      while (offset < bytes.length) {
        const first = bytes[offset];
        let length = 1;
        let codePoint = first;
        if (first >= 0xc2 && first <= 0xdf) {
          length = 2;
          codePoint = first & 0x1f;
        } else if (first >= 0xe0 && first <= 0xef) {
          length = 3;
          codePoint = first & 0x0f;
        } else if (first >= 0xf0 && first <= 0xf4) {
          length = 4;
          codePoint = first & 0x07;
        } else if (first >= 0x80) {
          text += '\ufffd';
          offset += 1;
          continue;
        }
        if (offset + length > bytes.length) {
          if (stream) break;
          text += '\ufffd';
          offset += 1;
          continue;
        }
        let valid = true;
        for (let index = 1; index < length; index += 1) {
          const next = bytes[offset + index];
          if ((next & 0xc0) !== 0x80) {
            valid = false;
            break;
          }
          codePoint = (codePoint << 6) | (next & 0x3f);
        }
        if (!valid
          || (length === 3 && codePoint < 0x800)
          || (length === 4 && (codePoint < 0x10000 || codePoint > 0x10ffff))
          || (codePoint >= 0xd800 && codePoint <= 0xdfff)) {
          text += '\ufffd';
          offset += 1;
          continue;
        }
        text += codePointToString(codePoint);
        offset += length;
      }
      pending = stream ? bytes.slice(offset) : new Uint8Array(0);
      return text;
    },
  };
};

export const createUtf8Decoder = () => (typeof globalThis.TextDecoder === 'function'
  ? new globalThis.TextDecoder('utf-8')
  : createFallbackUtf8Decoder());

export const createSseParser = (onEvent) => {
  const decoder = createUtf8Decoder();
  let buffer = '';
  const drain = () => {
    let match;
    while ((match = /\r?\n\r?\n/.exec(buffer))) {
      const frame = buffer.slice(0, match.index);
      buffer = buffer.slice(match.index + match[0].length);
      let type = 'message';
      const dataLines = [];
      frame.split(/\r?\n/).forEach((line) => {
        if (line.startsWith('event:')) type = line.slice(6).trim();
        if (line.startsWith('data:')) dataLines.push(line.slice(5).trimStart());
      });
      if (dataLines.length) onEvent({ type, data: JSON.parse(dataLines.join('\n')) });
    }
  };
  return {
    push(chunk) { buffer += decoder.decode(chunk, { stream: true }); drain(); },
    finish() { buffer += decoder.decode(); drain(); },
  };
};
