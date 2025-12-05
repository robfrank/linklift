import { ApiLinkRepository } from "./services/ApiLinkRepository";
import { ApiContentRepository } from "./services/ApiContentRepository";
import { ApiCollectionRepository } from "./services/ApiCollectionRepository";
import { AddLinkUseCase } from "../domain/usecases/AddLinkUseCase";

import { GetLinksUseCase } from "../domain/usecases/GetLinksUseCase";
import { DeleteLinkUseCase } from "../domain/usecases/DeleteLinkUseCase";
import { GetContentUseCase } from "../domain/usecases/GetContentUseCase";
import { RefreshContentUseCase } from "../domain/usecases/RefreshContentUseCase";
import { DeleteContentUseCase } from "../domain/usecases/DeleteContentUseCase";
import { GetCollectionsUseCase } from "../domain/usecases/GetCollectionsUseCase";
import { AddLinkToCollectionUseCase } from "../domain/usecases/AddLinkToCollectionUseCase";

export class DIContainer {
  private static _instance: DIContainer;

  private _linkRepository: ApiLinkRepository;
  private _contentRepository: ApiContentRepository;
  private _collectionRepository: ApiCollectionRepository;

  private constructor() {
    this._linkRepository = new ApiLinkRepository();
    this._contentRepository = new ApiContentRepository();
    this._collectionRepository = new ApiCollectionRepository();
  }

  public static getInstance(): DIContainer {
    if (!DIContainer._instance) {
      DIContainer._instance = new DIContainer();
    }
    return DIContainer._instance;
  }

  public resolveAddLinkUseCase(): AddLinkUseCase {
    return new AddLinkUseCase(this._linkRepository);
  }

  public resolveGetLinksUseCase(): GetLinksUseCase {
    return new GetLinksUseCase(this._linkRepository);
  }

  public resolveDeleteLinkUseCase(): DeleteLinkUseCase {
    return new DeleteLinkUseCase(this._linkRepository);
  }

  public resolveGetContentUseCase(): GetContentUseCase {
    return new GetContentUseCase(this._contentRepository);
  }

  public resolveRefreshContentUseCase(): RefreshContentUseCase {
    return new RefreshContentUseCase(this._contentRepository);
  }

  public resolveDeleteContentUseCase(): DeleteContentUseCase {
    return new DeleteContentUseCase(this._contentRepository);
  }

  public resolveGetCollectionsUseCase(): GetCollectionsUseCase {
    return new GetCollectionsUseCase(this._collectionRepository);
  }

  public resolveAddLinkToCollectionUseCase(): AddLinkToCollectionUseCase {
    return new AddLinkToCollectionUseCase(this._collectionRepository);
  }
}

export const container = DIContainer.getInstance();
