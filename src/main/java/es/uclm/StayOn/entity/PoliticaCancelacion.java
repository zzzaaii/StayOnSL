package es.uclm.StayOn.entity;

public enum PoliticaCancelacion {
    NO_REEMBOLSABLE,
    REEMBOLSABLE_50_PER,
    REEMBOLSABLE_100_PER;

    public double getPorcentajeReembolso() {
        return switch (this) {
            case NO_REEMBOLSABLE -> 0.0;
            case REEMBOLSABLE_50_PER -> 0.5;
            case REEMBOLSABLE_100_PER -> 1.0;
        };
    }
}
