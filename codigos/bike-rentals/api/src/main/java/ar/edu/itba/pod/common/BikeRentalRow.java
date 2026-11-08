package ar.edu.itba.pod.common;

import java.io.IOException;
import java.time.LocalDateTime;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;

public class BikeRentalRow implements DataSerializable {


    private String startStation, endStation;
    private LocalDateTime startDate, endDate;
    private boolean isMember;

    public BikeRentalRow(String startStation,
                         LocalDateTime startDate,
                         String endStation,
                         LocalDateTime endDate,
                         boolean isMember) {
        this.startStation = startStation;
        this.startDate = startDate;
        this.endStation = endStation;
        this.endDate = endDate;
        this.isMember = isMember;
    }

    public BikeRentalRow() {
    }

    public String getStartStation() {
        return startStation;
    }

    public String getEndStation() {
        return endStation;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public boolean isMember() {
        return isMember;
    }

    @Override
   public void writeData(ObjectDataOutput out) throws IOException {
       out.writeUTF(startStation);
       out.writeObject(startDate);
       out.writeUTF(endStation);
       out.writeObject(endDate);
       out.writeBoolean(isMember);
   }
   
   @Override
   public void readData(ObjectDataInput in) throws IOException {
       startStation = in.readUTF();
       startDate = in.readObject();
       endStation = in.readUTF();
       endDate = in.readObject();
       isMember = in.readBoolean();
   }

}
