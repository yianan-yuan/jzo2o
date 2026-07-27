const PENDING_ASSISTANT_TEXT = 'AI 正在思考…';

export const createPendingAssistantMessage = () => ({
  role: 'assistant',
  content: '',
  pending: true,
  recommendations: [],
  stage: '',
  error: null,
});

export const appendAssistantDelta = (message, text) => {
  message.pending = false;
  message.content += text || '';
};

export const displayAssistantContent = (message) => (
  message.pending ? PENDING_ASSISTANT_TEXT : message.content
);
