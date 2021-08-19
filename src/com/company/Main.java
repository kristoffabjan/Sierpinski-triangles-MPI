package com.company;

import mpi.MPI;
import mpi.MPIException;
import mpi.MaxInt;
import mpi.Status;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Main {
    //mogoce spemeni nazaj na static
    //----------------Konfiguracija programa--------------------------
    static int n = 2;  //st razcepov

    static int triangles_num = (int) Math.pow( 3, n );
    static boolean graphicsVisible = true;    //vklopi grafiko
    static boolean resizeAndZoom = true;      //vklopi zoom in resize
    //running mode: 1. sekvencno 2. paralelno 3. distributed 4.meritve in primerjave
    static int runningMode = 1;
    //----------------------------------------------------------------
    static double windowHeight = 600;  //sirina okna
    static double windowWidth = 800;   //visina okna
    static double startx;  //levo ogljisce x
    static double starty;  //levo ogljisce y
    static double lenght; //dolzina zacetne stranice
    static boolean flag = false;       //flag za spremembo zooma

    static public void main(String[] args) throws MPIException {
        MPI.Init(args);

        int myrank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size() ;
        int time = 2;

        if (myrank == 0){
            farmer1(size - 1);
        }else{
            worker1(myrank);
        }
        //System.out.println("Hello world from rank " + myrank + " of " + size );
        MPI.Finalize();
    }

    //TODO queue ne dela itd
    static void farmer1(int workers){
        //bag of tasks
        computeLength();
        setStartingPoint();
        int tag;
        int source;
        BlockingQueue<double[]> tasks = new LinkedBlockingQueue<double[]>();
        BlockingQueue<double[]> triangles = new LinkedBlockingQueue<double[]>();
        //public Task(double x, double y, double length, double level, double max_level, int source, int tag)
        double[] first_packet = new double[7];
        double[] incoming_task = new double[7];
        double[] outgoing_task = new double[7];
        int tasks_done = 0;

        //send first task to random worker
        int random_worker = (int) ((Math.random() * ((workers + 1) - 1)) + 1);
        first_packet = new double[]{startx, starty, lenght,  (double) 0, (double) n, (double)0, (double)1 };
        MPI.COMM_WORLD.Send(first_packet, 0, first_packet.length, MPI.DOUBLE, random_worker, 1);

        int i = 0;
        while (i < triangles_num ){
            MPI.COMM_WORLD.Recv(incoming_task, 0,incoming_task.length,MPI.DOUBLE, MPI.ANY_SOURCE,MPI.ANY_TAG);
            tag = (int) incoming_task[6];
            source = (int)  incoming_task[5];
           if (tag == 3){
               //finished triangle
               triangles.add(incoming_task);
               i++;
               System.out.println("Triangle with x coord = " + incoming_task[0] + " saved in queue");
           }else{
               //save new task to queue
               tasks.add(incoming_task);
               try {
                   outgoing_task = tasks.take();
                   System.out.println(outgoing_task[6]);
               } catch (InterruptedException e) {
                   e.printStackTrace();
                   System.out.println("Queue of task is empty");
               }
           }
            System.out.println(source + " source");
            MPI.COMM_WORLD.Send(outgoing_task,0,outgoing_task.length,MPI.DOUBLE, source, 1);
        }

        for (int j = 0; j < workers; j++) {
            MPI.COMM_WORLD.Recv(incoming_task, 0,incoming_task.length,MPI.DOUBLE, MPI.ANY_SOURCE, MPI.ANY_TAG);
            source = (int)incoming_task[5];
            tag = (int)incoming_task[6];
            if (tag == 3){
                triangles.add(incoming_task);
                System.out.println("Added another triangle in closing stage");
            }else {
                System.out.println("No more tasks : Queue size(tasks): " + tasks.size() + "  From worker "+ source);
            }
            double[] choke_array = new double[]{0.0, 0.0,0.0,0.0,0.0,0.0,404.0};
            MPI.COMM_WORLD.Send(choke_array, 0,choke_array.length,MPI.DOUBLE, source, tag);
            System.out.println("Work is done");
        }

    }

    static void worker1(int rank){
        int tasks_done = 0;
        double[] packet = new double[7];
        MPI.COMM_WORLD.Recv(packet, 0,packet.length, MPI.DOUBLE, 0, MPI.ANY_TAG);
        int tag = (int) packet[6];
        System.out.println("Worker "+ rank + " started.");

        while (tag != 404){
            double x = packet[0];
            double y = packet[1];
            double len = packet[2];
            double curr_lvl = packet[3];
            double max_lvl = packet[4];
            int source = (int) packet[5];
            tag = (int) packet[6];
            if (curr_lvl == max_lvl){
                //send triangle, tag = 3 = triangle done, send info source and tag=3
                double[] triangle = new double[]{x,y,len, 0.0, 0.0, 0.0, (double) rank, 3.0};
                MPI.COMM_WORLD.Send(triangle, 0, triangle.length, MPI.DOUBLE, 0, 3);
            }else {
                    //kot tag posljemo id workerja, da nam bo poslan nazaj isti task
                    double[] p1 = new double[]{x,y,len/2,curr_lvl +1, max_lvl, (double) rank, (double) tag};
                    MPI.COMM_WORLD.Send(p1, 0,p1.length, MPI.DOUBLE, 0, source);
                System.out.println("Worker "+ rank + " sent work back");

//                    double[] p2 = new double[]{x + len/4,y - (int)((Math.sin((double)Math.PI/3)) * (double)(len/2)),
//                            len/2,curr_lvl + 1,max_lvl,(double) rank, (double) tag};
//                    MPI.COMM_WORLD.Send(p2, 0,p2.length, MPI.DOUBLE, 0, source);
//                System.out.println("Worker "+ rank + " started.2222222");
//
//                    double[] p3 = new double[]{x+len/2,y,len/2,curr_lvl+1,max_lvl,(double) rank, (double) tag};
//                    MPI.COMM_WORLD.Send(p3, 0,p3.length, MPI.DOUBLE, 0, source);
            }
            MPI.COMM_WORLD.Recv(packet, 0, packet.length, MPI.DOUBLE, 0, MPI.ANY_TAG);
            tag = (int) packet[6];
        }

        System.out.println("Worker " + rank + " is done with job: Made " + tasks_done + " tasks." );
    }

    public void sierpinski(double x, double y, double len, double level, double max_level){
        if (level == max_level){
            if (true){
                //this.getChildren().add(trikotnikPolygon(x,y,len));
            }
        }else{
            sierpinski(x,y,len/2,level +1, max_level);
            sierpinski(x + len/4,y - (int)((Math.sin((double)Math.PI/3)) * (double)(len/2)),len/2,level +1, max_level);
            sierpinski(x + len/2,y,len/2,level +1, max_level);
        }
    }

    static void farmer(int workers){
        int[] tasks = new int[triangles_num];
        int[] results = new int[triangles_num];
        int[] msg = new int[3];
        int i;
        int[] temp = new int[3];
        int tag ,who;
        Status status = new Status();
        int choke = Integer.MAX_VALUE;
        System.out.println("Farmer started shuffling tasks: ....");

        for (int j = 0; j < triangles_num; j++) {
            //random int
            tasks[j] = (int)(5 + (Math.random() * 30));
        }
        System.out.println("Tasks created...");

        for (i = 0; i < workers ; i++) {
            //poslji farmerjem z id 1-5
            msg[0] = tasks[i];
            msg[1] = 0;
            msg[2] = i;
            MPI.COMM_WORLD.Send(msg,0,msg.length,MPI.INT, msg[2] + 1,msg[2] );
        }
        System.out.println("First task sent to all workers");

        while (i < triangles_num){
            MPI.COMM_WORLD.Recv(msg, 0,msg.length,MPI.INT, MPI.ANY_SOURCE,MPI.ANY_TAG);
            tag = msg[2];
            who = msg[1];
            results[tag] = msg[0];
            msg[0] = tasks[i];
            System.out.println("Farmer sent msg to " + who);
            System.out.println();
            MPI.COMM_WORLD.Send(msg,0,msg.length,MPI.INT, who, i);
            i++;
        }


        for (int j = 0; j < workers; j++) {
            MPI.COMM_WORLD.Recv(temp, 0,temp.length,MPI.INT, MPI.ANY_SOURCE, MPI.ANY_TAG);
            who = temp[1];
            tag = temp[2];
            //tag je v bistvu i, tag narasca
            results[tag] = temp[0];
            int[] choke_array = new int[]{0,0,choke};
            MPI.COMM_WORLD.Send(choke_array, 0,choke_array.length,MPI.INT, who,tag);
            System.out.println("Work is done");
        }

    }



    static void worker(int rank){
        int task_done = 0;
        int work_done = 0;
        int[] msg = new int[3];
        System.out.println("Worker " + rank + " started working...");


        MPI.COMM_WORLD.Recv(msg,0,msg.length,MPI.INT,0, MPI.ANY_TAG);
        //tag je st taska
        int tag = msg[2];
        while (tag != Integer.MAX_VALUE){
            work_done += msg[0];
            task_done++;
            msg[1] = rank;
            System.out.println("Worker " + rank + " working.....");
            MPI.COMM_WORLD.Send(msg, 0, msg.length, MPI.INT, 0,tag);
            System.out.println("sent by worker " + rank);
            MPI.COMM_WORLD.Recv(msg,0,msg.length,MPI.INT,0, MPI.ANY_TAG);
            tag = msg[2];

        }
        System.out.println("Worker " + rank + " is done with job: Made " +work_done + " work." );
    }

    static void setStartingPoint(){
        startx = (windowWidth - lenght)/2;
        double newY = windowHeight - (windowHeight - (int)((double)lenght*Math.sqrt(3.0)/2.0)) / 2  ;
        starty = newY - (newY/25);
        //(height - visina trikotnika)/2
    }

    static void computeLength(){
        double max_tri_height = windowHeight - (windowHeight/10);
        lenght = max_tri_height/(Math.sin(Math.PI/3));

        if (windowWidth <= lenght){
            windowWidth = lenght + 50 ;
        }
    }
}
