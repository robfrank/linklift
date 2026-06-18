import { Tag, CreateTagDTO, AddTagToLinkDTO } from "../models/Tag";

export interface ITagRepository {
  listTags(): Promise<Tag[]>;
  createTag(data: CreateTagDTO): Promise<Tag>;
  deleteTag(tagId: string): Promise<void>;
  getTagsForLink(linkId: string): Promise<Tag[]>;
  addTagToLink(linkId: string, data: AddTagToLinkDTO): Promise<void>;
  removeTagFromLink(linkId: string, tagId: string): Promise<void>;
  suggestTags(linkId: string): Promise<Tag[]>;
}
