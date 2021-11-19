package main.java.model.geometry.solar;

public class AzimuthZenithAngle {
    private final double azimuth;
    private final double zenithAngle;

    public AzimuthZenithAngle(final double azimuth, final double zenithAngle) {
        this.zenithAngle = zenithAngle;
        this.azimuth = azimuth;
    }

    public final double getZenithAngle() {
        return zenithAngle;
    }

    public final double getAzimuth() {
        return azimuth;
    }

    @Override
    public String toString() {
        return String.format("azimuth %.6f degree, zenith angle %.6f degree", azimuth, zenithAngle);
    }
}
