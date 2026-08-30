package at.sari.trader.validation;

public record ValidationCriterion(String name, boolean passed, String actual, String requirement) {}
