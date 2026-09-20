package entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

import java.util.ArrayList;
import java.util.List;

@Entity
public class Tray
{
    @Id
    private String trayId;

    private String partType;
    private double maxWeight;
    private double currentWeight;

    @OneToMany(mappedBy = "tray")
    private List<AnimalPart> parts = new ArrayList<>();

    public String getTrayId()
    {

        return trayId;
    }

    public void setTrayId(String trayId)
    {
        this.trayId = trayId;
    }

    public String getPartType()
    {
        return partType;
    }

    public void setPartType(String partType)
    {
        this.partType = partType;
    }

    public double getMaxWeight()
    {
        return maxWeight;
    }

    public void setMaxWeight(double maxWeight)
    {
        this.maxWeight = maxWeight;
    }

    public double getCurrentWeight()
    {
        return currentWeight;
    }

    public List<AnimalPart> getParts()
    {
        return parts;
    }

    public void addPart(AnimalPart part)
    {
        parts.add(part);
        part.setTray(this);
        currentWeight += part.getWeight();
    }

    public void removePart(AnimalPart part)
    {
        if (parts.remove(part))
        {
            part.setTray(null);
            currentWeight -= part.getWeight();
        }
    }

}
