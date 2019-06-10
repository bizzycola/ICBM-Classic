package icbm.classic.content.machines.radarstation;

public class CCReturnRadarStation {
    public double X;
    public double Y;
    public double Z;
    public String Name;

    public CCReturnRadarStation(){}
    public CCReturnRadarStation(double x, double y, double z, String name) {
        X = x;
        Y = y;
        Z = z;

        Name = name;
    }
}
