
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import java.util.ArrayList;
import java.util.List;
public class SJFDatacenterBroker extends DatacenterBroker {
 SJFDatacenterBroker(String name) throws Exception {
 super(name);
 }
 public void scheduleTaskstoVms() {
 int reqTasks = cloudletList.size();
 int reqVms = vmList.size();
 Vm vm = vmList.get(0);
 for (int i = 0; i < reqTasks; i++) {
 bindCloudletToVm(i, (i % reqVms));
 System.out.println("Task" + cloudletList.get(i).getCloudletId() + " is bound with VM" + vmList.get(i % reqVms).getId());
 }
 //System.out.println("reqTasks: "+ reqTasks);
 ArrayList<Cloudlet> list = new ArrayList<Cloudlet>();
 for (Cloudlet cloudlet : getCloudletReceivedList()) {
 list.add(cloudlet);
 }
 //setCloudletReceivedList(null);
 Cloudlet[] list2 = list.toArray(new Cloudlet[list.size()]);
 //System.out.println("size :"+list.size());
 Cloudlet temp = null;
 int n = list.size();
 for (int i = 0; i < n; i++) {
 for (int j = 1; j < (n - i); j++) {
 if (list2[j - 1].getCloudletLength() / (vm.getMips() * vm.getNumberOfPes()) > list2[j].getCloudletLength() /
(vm.getMips() * vm.getNumberOfPes())) {
 //swap the elements!
 //swap(list2[j-1], list2[j]);
 temp = list2[j - 1];
 list2[j - 1] = list2[j];
 list2[j] = temp;
 }
 // printNumbers(list2);
 }
 }
 ArrayList<Cloudlet> list3 = new ArrayList<Cloudlet>();
 for (int i = 0; i < list2.length; i++) {
 list3.add(list2[i]);
 }
 //printNumbers(list);
 setCloudletReceivedList(list);
 //System.out.println("\n\tSJFS Broker Schedules\n");
 //System.out.println("\n");
 }
 public void printNumber(Cloudlet[] list) {
 for (int i = 0; i < list.length; i++) {
 System.out.print(" " + list[i].getCloudletId());
 System.out.println(list[i].getCloudletStatusString());
 }
 System.out.println();
 }
 public void printNumbers(ArrayList<Cloudlet> list) {
 for (int i = 0; i < list.size(); i++) {
 System.out.print(" " + list.get(i).getCloudletId());
 }
 System.out.println();
 }
 @Override
 protected void processCloudletReturn(SimEvent ev) {
 Cloudlet cloudlet = (Cloudlet) ev.getData();
 getCloudletReceivedList().add(cloudlet);
 Log.printLine(CloudSim.clock() + ": " + getName() + ": Cloudlet " + cloudlet.getCloudletId()
 + " received");
 cloudletsSubmitted--;
 if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) {
 scheduleTaskstoVms();
 cloudletExecution(cloudlet);
 }
 }
 protected void cloudletExecution(Cloudlet cloudlet) {
 if (getCloudletList().size() == 0 && cloudletsSubmitted == 0) { // all cloudlets executed
 Log.printLine(CloudSim.clock() + ": " + getName() + ": All Cloudlets executed. Finishing...");
 clearDatacenters();
 finishExecution();
 } else { // some cloudlets haven't finished yet
 if (getCloudletList().size() > 0 && cloudletsSubmitted == 0) {
 // all the cloudlets sent finished. It means that some bount
 // cloudlet is waiting its VM be created
 clearDatacenters();
 createVmsInDatacenter(0);
 }
 }
 }
 @Override
 protected void processResourceCharacteristics(SimEvent ev) {
 DatacenterCharacteristics characteristics = (DatacenterCharacteristics) ev.getData();
 getDatacenterCharacteristicsList().put(characteristics.getId(), characteristics);
 if (getDatacenterCharacteristicsList().size() == getDatacenterIdsList().size()) {
 distributeRequestsForNewVmsAcrossDatacenters();
 }
 }
 protected void distributeRequestsForNewVmsAcrossDatacenters() {
 int numberOfVmsAllocated = 0;
 int i = 0;
 final List<Integer> availableDatacenters = getDatacenterIdsList();
 for (Vm vm : getVmList()) {
 int datacenterId = availableDatacenters.get(i++ % availableDatacenters.size());
 String datacenterName = CloudSim.getEntityName(datacenterId);
 if (!getVmsToDatacentersMap().containsKey(vm.getId())) {
 Log.printLine(CloudSim.clock() + ": " + getName() + ": Trying to Create VM #" + vm.getId() + " in " + datacenterName);
 sendNow(datacenterId, CloudSimTags.VM_CREATE_ACK, vm);
 numberOfVmsAllocated++;
 }
 }
 setVmsRequested(numberOfVmsAllocated);
 setVmsAcks(0);
 }
SJF_Scheduler.java
package <
package_name>;
import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
//import utils.Constants;
//import utils.DatacenterCreator;
//import utils.GenerateMatrices;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
public class SJF_Scheduler {
 private static List<Cloudlet> cloudletList;
 private static List<Vm> vmList;
 private static Datacenter[] datacenter;
 private static double[][] commMatrix;
 private static double[][] execMatrix;
 private static List<Vm> createVM(int userId, int vms) {
 //Creates a container to store VMs. This list is passed to the broker later
 LinkedList<Vm> list = new LinkedList<Vm>();
 //VM Parameters
 long size = 10000; //image size (MB)
 int ram = 512; //vm memory (MB)
 int mips = 250;
 long bw = 1000;
 int pesNumber = 1; //number of cpus
 String vmm = "Xen"; //VMM name
 //create VMs
 Vm[] vm = new Vm[vms];
 for (int i = 0; i < vms; i++) {
 vm[i] = new Vm(datacenter[i].getId(), userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerSpaceShared());
 list.add(vm[i]);
 }
 return list;
 }
 private static List<Cloudlet> createCloudlet(int userId, int cloudlets, int idShift) {
 // Creates a container to store Cloudlets
 LinkedList<Cloudlet> list = new LinkedList<Cloudlet>();
 //cloudlet parameters
 long fileSize = 300;
 long outputSize = 300;
 int pesNumber = 1;
 UtilizationModel utilizationModel = new UtilizationModelFull();
 Cloudlet[] cloudlet = new Cloudlet[cloudlets];
 for (int i = 0; i < cloudlets; i++) {
 int dcId = (int) (Math.random() * Constants.NO_OF_DATA_CENTERS);
 long length = (long) (1e3 * (commMatrix[i][dcId] + execMatrix[i][dcId]));
 cloudlet[i] = new Cloudlet(idShift + i, length, pesNumber, fileSize, outputSize, utilizationModel, utilizationModel, utilizationModel);
 // setting the owner of these Cloudlets
 cloudlet[i].setUserId(userId);
 cloudlet[i].setVmId(dcId + 2);
 list.add(cloudlet[i]);
 }
 return list;
 }
 public static void main(String[] args) {
 Log.printLine("Starting SJF Scheduler...");
 new GenerateMatrices();
 execMatrix = GenerateMatrices.getExecMatrix();
 commMatrix = GenerateMatrices.getCommMatrix();
 try {
 int num_user = 1; // number of grid users
 Calendar calendar = Calendar.getInstance();
 boolean trace_flag = false; // mean trace events
 CloudSim.init(num_user, calendar, trace_flag);
 // Second step: Create Datacenters
 datacenter = new Datacenter[Constants.NO_OF_DATA_CENTERS];
 for (int i = 0; i < Constants.NO_OF_DATA_CENTERS; i++) {
 datacenter[i] = DatacenterCreator.createDatacenter("Datacenter_" + i);
 }
 //Third step: Create Broker
 SJFDatacenterBroker broker = createBroker("Broker_0");
 int brokerId = broker.getId();
 //Fourth step: Create VMs and Cloudlets and send them to broker
 vmList = createVM(brokerId, Constants.NO_OF_DATA_CENTERS);
 cloudletList = createCloudlet(brokerId, Constants.NO_OF_TASKS, 0);
 broker.submitVmList(vmList);
 broker.submitCloudletList(cloudletList);
 // Fifth step: Starts the simulation
 CloudSim.startSimulation();
 // Final step: Print results when simulation is over
 List<Cloudlet> newList = broker.getCloudletReceivedList();
 //newList.addAll(globalBroker.getBroker().getCloudletReceivedList());
 CloudSim.stopSimulation();
 printCloudletList(newList);
 Log.printLine(SJF_Scheduler.class.getName() + " finished!");
 } catch (Exception e) {
 e.printStackTrace();
 Log.printLine("The simulation has been terminated due to an unexpected error");
 }
 }
 private static SJFDatacenterBroker createBroker(String name) throws Exception {
 return new SJFDatacenterBroker(name);
 }
 /**
 * Prints the Cloudlet objects
 *
 * @param list list of Cloudlets
 */
 private static void printCloudletList(List<Cloudlet> list) {
 int size = list.size();
 Cloudlet cloudlet;
 String indent = " ";
 Log.printLine();
 Log.printLine("========== OUTPUT ==========");
 Log.printLine("Cloudlet ID" + indent + "STATUS" +
 indent + "Data center ID" +
 indent + "VM ID" +
 indent + indent + "Time" +
 indent + "Start Time" +
 indent + "Finish Time" +
 indent + "Waiting Time");
 DecimalFormat dft = new DecimalFormat("###.##");
 dft.setMinimumIntegerDigits(2);
 for (int i = 0; i < size; i++) {
 cloudlet = list.get(i);
 Log.print(indent + dft.format(cloudlet.getCloudletId()) + indent + indent);
 if (cloudlet.getCloudletStatus() == Cloudlet.SUCCESS) {
 Log.print("SUCCESS");
 Log.printLine(indent + indent + dft.format(cloudlet.getResourceId()) +
 indent + indent + indent + dft.format(cloudlet.getVmId()) +
 indent + indent + dft.format(cloudlet.getActualCPUTime()) +
 indent + indent + dft.format(cloudlet.getExecStartTime()) +
 indent + indent + indent + dft.format(cloudlet.getFinishTime())+
 indent + indent + indent + dft.format(cloudlet.getWaitingTime()));
 }
 }
 double makespan = calcMakespan(list);
 Log.printLine("Makespan using SJF: " + makespan);
 }
 private static double calcMakespan(List<Cloudlet> list) {
 double makespan = 0;
 double[] dcWorkingTime = new double[Constants.NO_OF_DATA_CENTERS];
 for (int i = 0; i < Constants.NO_OF_TASKS; i++) {
 int dcId = list.get(i).getVmId() % Constants.NO_OF_DATA_CENTERS
 if (dcWorkingTime[dcId] != 0) --dcWorkingTime[dcId];
 dcWorkingTime[dcId] += execMatrix[i][dcId] + commMatrix[i][dcId];
 makespan = Math.max(makespan, dcWorkingTime[dcId]);
 }
 return makespan;
 }
}