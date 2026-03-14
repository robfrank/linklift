import { Link, UpdateLinkStatusDTO } from "../models/Link";
import { ILinkRepository } from "../ports/ILinkRepository";

export class UpdateLinkStatusUseCase {
  constructor(private readonly repository: ILinkRepository) {}

  async execute(id: string, status: UpdateLinkStatusDTO): Promise<Link> {
    return this.repository.updateStatus(id, status);
  }
}
