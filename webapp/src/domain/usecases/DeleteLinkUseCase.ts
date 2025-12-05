import { ILinkRepository } from "../ports/ILinkRepository";

export class DeleteLinkUseCase {
  constructor(private linkRepository: ILinkRepository) {}

  async execute(id: string): Promise<void> {
    return this.linkRepository.delete(id);
  }
}
