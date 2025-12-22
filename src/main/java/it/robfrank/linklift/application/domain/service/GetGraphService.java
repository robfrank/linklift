package it.robfrank.linklift.application.domain.service;

import it.robfrank.linklift.application.domain.model.GraphData;
import it.robfrank.linklift.application.port.in.GetGraphUseCase;
import it.robfrank.linklift.application.port.out.LoadLinksPort;

public class GetGraphService implements GetGraphUseCase {

  private final LoadLinksPort loadLinksPort;

  public GetGraphService(LoadLinksPort loadLinksPort) {
    this.loadLinksPort = loadLinksPort;
  }

  @Override
  public GraphData getGraphData(String userId) {
    return loadLinksPort.getGraphData(userId);
  }
}
