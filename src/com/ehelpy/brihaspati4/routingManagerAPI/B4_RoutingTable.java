package com.ehelpy.brihaspati4.routingManagerAPI;

/**
 * This class is used to create an object of B4_routingTable.
 * It contains routing an neighbour table parameters of a node.
 * This class will create a routing table and neighbour table array based on the dimension provided as the argument.
 */
 class B4_RoutingTable {
    private final B4_Node[][] routingTable;
    private final B4_Node[] neighbourTable;

    /**
     * @param rt_dimension - routing table dimension is taken as argument.
     * @param nt_dimension - neighbour table dimension is taken as argument.
     */
    B4_RoutingTable(int rt_dimension,int nt_dimension) {
        this.routingTable = new B4_Node[rt_dimension][3];
        this.neighbourTable = new B4_Node[nt_dimension];
    }

    /**
     * @return - routingTable
     */
    B4_Node[][] getRoutingTable() {
        return routingTable;
    }

    /**
     * @return - neighbourTable
     */
    B4_Node[] getNeighbourTable() {
        return neighbourTable;
    }
}
