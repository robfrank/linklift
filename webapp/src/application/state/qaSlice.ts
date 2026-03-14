import { StateCreator } from "zustand";
import { ConversationEntry, QuestionAnswer } from "../../domain/models/QA";
import api from "../../infrastructure/api/axios-instance";

export interface QASlice {
  conversation: ConversationEntry[];
  isAsking: boolean;
  askError: string | null;
  ask: (question: string) => Promise<void>;
  clearConversation: () => void;
}

export const createQASlice: StateCreator<QASlice> = (set) => ({
  conversation: [],
  isAsking: false,
  askError: null,

  ask: async (question: string) => {
    set({ isAsking: true, askError: null });
    try {
      const response = await api.post<QuestionAnswer>("/api/v1/ask", { question });
      const entry: ConversationEntry = {
        id: crypto.randomUUID(),
        question: response.data.question,
        answer: response.data.answer,
        sources: response.data.sources,
        timestamp: new Date()
      };
      set((state) => ({ conversation: [...state.conversation, entry] }));
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : "Failed to get an answer";
      set({ askError: message });
    } finally {
      set({ isAsking: false });
    }
  },

  clearConversation: () => set({ conversation: [], askError: null })
});
