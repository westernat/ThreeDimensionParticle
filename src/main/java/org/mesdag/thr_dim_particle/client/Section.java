package org.mesdag.thr_dim_particle.client;

import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.AABB;

public class Section {
    private final int x;
    private final int y;
    private final int z;
    private AABB box;

    Section(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Section of(double wx, double wy, double wz) {
        return new Section(
                SectionPos.posToSectionCoord(wx),
                SectionPos.posToSectionCoord(wy),
                SectionPos.posToSectionCoord(wz)
        );
    }

    public AABB getBox() {
        if (box == null) {
            double mx = SectionPos.sectionToBlockCoord(x);
            double my = SectionPos.sectionToBlockCoord(y);
            double mz = SectionPos.sectionToBlockCoord(z);
            this.box = new AABB(mx, my, mz, mx + 16, my + 16, mz + 16);
        }
        return box;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }

    @Override
    public boolean equals(Object o) {
        return o == this || (o instanceof Section section && section.x == x && section.y == y && section.z == z);
    }

    @Override
    public String toString() {
        return "Section{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }
}
