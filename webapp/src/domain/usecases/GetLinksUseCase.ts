import { ILinkRepository } from "../ports/ILinkRepository";
import { Link } from "../models/Link";
import { Page, PageRequest } from "../models/Page";

export class GetLinksUseCase {
  constructor(private linkRepository: ILinkRepository) {}

  async execute(request: PageRequest): Promise<Page<Link>> {
    return this.linkRepository.getAll(request);
  }
}
