import test from 'node:test';
import assert from 'node:assert/strict';
import { keepOrderId } from '../utils/orderId.js';

test('keeps a 19-digit route order id unchanged for the evaluation request', () => {
  assert.equal(keepOrderId('2607300000000000238'), '2607300000000000238');
});
