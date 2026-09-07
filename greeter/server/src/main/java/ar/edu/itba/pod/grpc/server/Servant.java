package ar.edu.itba.pod.grpc.server;

import org.springframework.stereotype.Service;

import ar.edu.itba.pod.grpc.GreeterGrpc.GreeterImplBase;
import ar.edu.itba.pod.grpc.HelloReply;
import ar.edu.itba.pod.grpc.HelloRequest;
import io.grpc.stub.StreamObserver;

@Service
public class Servant extends GreeterImplBase {

	@Override
	public void sayHello(HelloRequest req, StreamObserver<HelloReply> responseObserver) {
		HelloReply reply = HelloReply.newBuilder()
				.setMessage("Hello " + req.getName())
				.build();
		responseObserver.onNext(reply);
		responseObserver.onCompleted();
	}

}
