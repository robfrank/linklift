import { StateCreator } from "zustand";
import { ApiTagRepository } from "../services/ApiTagRepository";
import { Tag } from "../../domain/models/Tag";

const tagRepository = new ApiTagRepository();

export interface TagSlice {
  // All user tags
  tags: Tag[];
  isLoadingTags: boolean;
  tagsError: string | null;
  fetchTags: () => Promise<void>;

  // Tags for a specific link
  linkTags: Record<string, Tag[]>;
  fetchTagsForLink: (linkId: string) => Promise<void>;

  // CRUD
  createTag: (name: string) => Promise<Tag | null>;
  deleteTag: (tagId: string) => Promise<void>;
  addTagToLink: (linkId: string, tagId: string) => Promise<void>;
  removeTagFromLink: (linkId: string, tagId: string) => Promise<void>;

  // Suggestions
  suggestedTags: Tag[];
  fetchSuggestedTags: (linkId: string) => Promise<void>;
}

export const createTagSlice: StateCreator<TagSlice> = (set, get) => ({
  tags: [],
  isLoadingTags: false,
  tagsError: null,

  fetchTags: async () => {
    set({ isLoadingTags: true, tagsError: null });
    try {
      const tags = await tagRepository.listTags();
      set({ tags, isLoadingTags: false });
    } catch (error: any) {
      set({ isLoadingTags: false, tagsError: "Failed to load tags" });
    }
  },

  linkTags: {},

  fetchTagsForLink: async (linkId) => {
    try {
      const tags = await tagRepository.getTagsForLink(linkId);
      set((state) => ({ linkTags: { ...state.linkTags, [linkId]: tags } }));
    } catch (error: any) {
      // Don't surface per-link tag errors prominently
    }
  },

  createTag: async (name) => {
    try {
      const tag = await tagRepository.createTag({ name });
      set((state) => ({ tags: [...state.tags.filter((t) => t.id !== tag.id), tag].sort((a, b) => a.name.localeCompare(b.name)) }));
      return tag;
    } catch (error: any) {
      return null;
    }
  },

  deleteTag: async (tagId) => {
    await tagRepository.deleteTag(tagId);
    set((state) => ({ tags: state.tags.filter((t) => t.id !== tagId) }));
  },

  addTagToLink: async (linkId, tagId) => {
    await tagRepository.addTagToLink(linkId, { tagId });
    await get().fetchTagsForLink(linkId);
  },

  removeTagFromLink: async (linkId, tagId) => {
    await tagRepository.removeTagFromLink(linkId, tagId);
    set((state) => ({
      linkTags: {
        ...state.linkTags,
        [linkId]: (state.linkTags[linkId] || []).filter((t) => t.id !== tagId)
      }
    }));
  },

  suggestedTags: [],

  fetchSuggestedTags: async (linkId) => {
    try {
      const suggestions = await tagRepository.suggestTags(linkId);
      set({ suggestedTags: suggestions });
    } catch (error: any) {
      set({ suggestedTags: [] });
    }
  }
});
