package it.robfrank.linklift.application.port.in;

public record NewLinkCommand(String url, String title, String description) {}
