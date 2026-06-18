import { Link, CreateLinkDTO, UpdateLinkStatusDTO } from "../models/Link";
import { Page, PageRequest } from "../models/Page";

export interface ILinkRepository {
  create(link: CreateLinkDTO): Promise<Link>;
  getAll(request: PageRequest): Promise<Page<Link>>;
  delete(id: string): Promise<void>;
  updateStatus(id: string, status: UpdateLinkStatusDTO): Promise<Link>;
}
