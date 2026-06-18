export interface AnswerSource {
  linkId: string;
  title: string;
  url: string;
  excerpt: string | null;
}

export interface QuestionAnswer {
  question: string;
  answer: string;
  sources: AnswerSource[];
}

export interface AskQuestionDTO {
  question: string;
}

export interface ConversationEntry {
  id: string;
  question: string;
  answer: string;
  sources: AnswerSource[];
  timestamp: Date;
}
