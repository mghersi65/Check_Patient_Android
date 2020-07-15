package ch.uepaa.quickstart;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.persistence.DataQueryBuilder;

import java.util.List;
import java.util.UUID;

public class User
{

    private java.util.Date created;
    private java.util.Date updated;
    private String objectId;
    private String ownerId;
    private String dni;
    private String name;
    private String profileImageUrl;
    private String user;
    private String peerId;
    private String deviceId;


    public java.util.Date getCreated()
    {
        return created;
    }
    public java.util.Date getUpdated()
    {
        return updated;
    }

    public String getDni()
    {
        return dni;
    }
    public void setDni( String dni ) { this.dni = dni; }

    public String getName()
    {
        return name;
    }
    public void setName( String name )
    {
        this.name = name;
    }

    public String getObjectId()
    {
        return objectId;
    }
    public void setObjectId( String objectId )
    {
        this.objectId = objectId;
    }

    public String getProfileImageUrl()
    {
        return profileImageUrl;
    }
    public void setProfileImageUrl(String profileImageUrl )
    {
        this.profileImageUrl = profileImageUrl;
    }

    public String getUser()
    {
        return user;
    }
    public void setUser(String user )
    {
        this.user = user;
    }

    public String getOwnerId()
    {
        return ownerId;
    }
    public void setOwnerId(String ownerId )
    {
        this.ownerId = ownerId;
    }

    public String getPeerId()
    {
        return peerId;
    }
    public void setPeerId(String peerId )
    {
        this.peerId = peerId;
    }

    public String getDeviceId()
    {
        return deviceId;
    }
    public void setDeviceId(String deviceId )
    {
        this.deviceId = deviceId;
    }


}