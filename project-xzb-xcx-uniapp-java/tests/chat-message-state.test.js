import test from 'node:test';
import assert from 'node:assert/strict';
import {
  appendAssistantDelta,
  createPendingAssistantMessage,
  displayAssistantContent,
} from '../pages/ai-chat/chat-message-state.js';

test('shows one pending assistant bubble until the first streamed delta arrives', () => {
  const message = createPendingAssistantMessage();

  assert.equal(displayAssistantContent(message), 'AI 正在思考…');
  appendAssistantDelta(message, '您好');

  assert.equal(message.pending, false);
  assert.equal(displayAssistantContent(message), '您好');
});
