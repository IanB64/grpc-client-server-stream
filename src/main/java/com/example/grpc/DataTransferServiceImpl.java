package com.example.grpc;

import com.example.grpc.dataTransfer.DataChunk;
import com.example.grpc.dataTransfer.DataTransferServiceGrpc;
import com.example.grpc.dataTransfer.Reply;
import com.google.protobuf.ByteString;

import io.grpc.stub.StreamObserver;

public class DataTransferServiceImpl extends DataTransferServiceGrpc.DataTransferServiceImplBase {
  @Override
  public StreamObserver<DataChunk> send(final StreamObserver<Reply> responseObserver) {
    return new StreamObserver<DataChunk>() {
      int totalDataSize = 0;

      @Override
      public void onNext(DataChunk data) {
        ByteString dataChunk = data.getData();
        totalDataSize += dataChunk.size();
        Reply reply = Reply.newBuilder().setReply("" + totalDataSize).build();
        responseObserver.onNext(reply);
      }

      @Override
      public void onError(Throwable t) {
        t.printStackTrace();
      }

      @Override
      public void onCompleted() {
        System.out.println("Byte received: " + totalDataSize + " bytes.");
        Reply reply = Reply.newBuilder().setReply(totalDataSize + " bytes received by server.").build();
        responseObserver.onNext(reply);
        responseObserver.onCompleted();
      }
    };
  }
}
