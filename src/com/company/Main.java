package com.company;

import mpi.MPI;
import mpi.MPIException;
import mpi.Status;

public class Main {
    //mogoce spemeni nazaj na static
    static int tasks_num = 5;

    static public void main(String[] args) throws MPIException {
        MPI.Init(args);

        int myrank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size() ;
        int time = 2;

        if (myrank == 0){
            farmer(size - 1);
        }else{
            worker(myrank);
        }
        //System.out.println("Hello world from rank " + myrank + " of " + size );
        MPI.Finalize();
    }

    static void farmer(int workers){
        int[] tasks = new int[tasks_num];
        int[] results = new int[tasks_num];
        int[] msg = new int[3];
        int i;
        int[] temp = new int[3];
        int tag ,who;
        Status status = new Status();
        int choke = Integer.MAX_VALUE;
        System.out.println("Farmer started shuffling tasks: ....");

        for (int j = 0; j < tasks_num; j++) {
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

        while (i < tasks_num){
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
}
