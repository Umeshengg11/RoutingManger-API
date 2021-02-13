package main;

public class B4_RoutingTable {
    private final B4_Node[][] routingTable;
    private final B4_Node[] neighbourTable;

    public B4_RoutingTable(int rt_dimension,int nt_dimension) {
        this.routingTable = new B4_Node[rt_dimension][3];
        this.neighbourTable = new B4_Node[nt_dimension];
    }

    public B4_Node[][] getRoutingTable() {
        return routingTable;
    }

    public B4_Node[] getNeighbourTable() {
        return neighbourTable;
    }
}
