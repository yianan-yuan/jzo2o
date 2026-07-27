import test from 'node:test';
import assert from 'node:assert/strict';
import { shouldOpenLocationSetting } from '../utils/location-authorization.js';

test('requests location directly when the user has not made an authorization choice', () => {
  assert.equal(shouldOpenLocationSetting({}), false);
});

test('opens settings only after location permission was explicitly denied', () => {
  assert.equal(shouldOpenLocationSetting({ 'scope.userLocation': false }), true);
});

test('requests location directly when permission is already granted', () => {
  assert.equal(shouldOpenLocationSetting({ 'scope.userLocation': true }), false);
});
