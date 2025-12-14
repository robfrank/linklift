import { ILinkRepository } from "../ports/ILinkRepository";
import { CreateLinkDTO, Link } from "../models/Link";

export class AddLinkUseCase {
  constructor(private linkRepository: ILinkRepository) {}

  async execute(linkData: CreateLinkDTO): Promise<Link> {
    // We could add business validation here if needed
    // e.g. check if URL is valid (though UI should also do it, domain must strictly enforce it)
    return this.linkRepository.create(linkData);
  }
}
