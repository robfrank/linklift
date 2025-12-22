package it.robfrank.linklift.application.port.in;

import it.robfrank.linklift.application.domain.model.GraphData;

public interface GetGraphUseCase {
  GraphData getGraphData(String userId);
}
