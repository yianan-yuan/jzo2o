import test from 'node:test';
import assert from 'node:assert/strict';
import { buildEvaluationPageParams } from '../utils/evaluation-query.js';

test('all evaluations omit evaluationType instead of sending a null filter', () => {
  assert.deepEqual(buildEvaluationPageParams({ pageNo: 1, pageSize: 10, evaluationType: null }), {
    pageNo: 1,
    pageSize: 10
  });
});

test('good and bad filters retain their evaluation type', () => {
  assert.deepEqual(buildEvaluationPageParams({ pageNo: 2, pageSize: 20, evaluationType: 1 }), {
    pageNo: 2,
    pageSize: 20,
    evaluationType: 1
  });
});
