package com.example.grpc;

import java.io.IOException;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import com.example.grpc.dataTransfer.DataChunk;
import com.example.grpc.dataTransfer.DataTransferServiceGrpc;
import com.example.grpc.dataTransfer.Reply;
import com.google.protobuf.ByteString;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

// mvn exec:java -Dexec.mainClass=com.example.grpc.DataClient -Dexec.args="hostaddr port packetsize datasize"
public class DataClient {
  public static void main(String[] args) throws InterruptedException, IOException {
    String hostAddr = args[0];
    int port = Integer.parseInt(args[1]);
    int packetsize = Integer.parseInt(args[2]);
    final int size = Integer.parseInt(args[3]);

    final ManagedChannel channel = ManagedChannelBuilder.forAddress(hostAddr, port).usePlaintext().build();
    DataTransferServiceGrpc.DataTransferServiceStub dataTransferService = DataTransferServiceGrpc.newStub(channel);

    int numPackets = 0;
    byte[] data;
    int totalByteSent = 0;
    long startTime, endTime;

    StreamObserver<DataChunk> dataChunkObserver = dataTransferService.send(new StreamObserver<Reply>() {
      @Override
      public void onNext(Reply reply) {
        // do nothing
        // System.out.println(reply.getReply());
        if(reply.getReply().equals(Integer.toString(size) + " bytes received by server.")){
          System.out.println(reply.getReply());
        }
      };

      @Override
      public void onError(Throwable t) {
        t.printStackTrace();
      }

      @Override
      public void onCompleted() {
        channel.shutdown();
      }
    });

    int current = 0;
    try {
      startTime = System.nanoTime();
      System.out.println("Start time: " + startTime);
      while (current != size) {
        numPackets++;
        if (size - current >= packetsize)
          current += packetsize;
        else {
          packetsize = (int) (size - current);
          current = size;
        }

        data = new byte[packetsize];
        totalByteSent += packetsize;
        DataChunk dataChunk = DataChunk.newBuilder().setData(ByteString.copyFrom(data, 0, packetsize)).build();
        dataChunkObserver.onNext(dataChunk);
        System.out.print("*");
      }
    } catch (Exception e) {
      dataChunkObserver.onError(e);
      return;
    }
    dataChunkObserver.onCompleted();

    endTime = System.nanoTime();
	
	System.out.println("\n"+ numPackets + " packets sent. " + totalByteSent + " bytes of total data sent.");
    System.out.println("\nEnd time: " + endTime);
    long duration = (endTime - startTime);
    double ms = duration / 1000000.0;
    System.out.println("Duration: " + ms + " ms");

    double throughput = (double) current * (1000000000L) / duration;
    System.out.println("Throughput: " + getFormatSize(throughput) + "/s");
  }

  private static DecimalFormat df = null;

  static {
    // set format
    df = new DecimalFormat("#0.0");
    df.setRoundingMode(RoundingMode.HALF_UP);
    df.setMaximumFractionDigits(1);
    df.setMinimumFractionDigits(1);
  }

  // format
  static private String getFormatSize(double length) {
    double size = length / (1 << 30);
    if (size >= 1) {
      return df.format(size) + "GB";
    }
    size = length / (1 << 20);
    if (size >= 1) {
      return df.format(size) + "MB";
    }
    size = length / (1 << 10);
    if (size >= 1) {
      return df.format(size) + "KB";
    }
    return df.format(length) + "B";
  }
}
