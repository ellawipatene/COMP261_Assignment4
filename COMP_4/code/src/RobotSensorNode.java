/*
 * Interface for conditional sensor nodes that can be evaluated,
 * including the top level program node
 */

interface RobotSensorNode {
	public int evaluate(Robot robot);
}
