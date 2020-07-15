package ch.uepaa.quickstart;

import android.content.Intent;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.persistence.DataQueryBuilder;

import java.util.List;
import java.util.UUID;

public class Patients
{

    private java.util.Date created;
    private java.util.Date updated;
    private String objectId;
    private String ownerIdDoc;
    private String ownerIdPac;
    private String dniDoc;
    private String dniPac;
    private String nameDoc;
    private String namePac;
    private String profileImageUrlDoc;
    private String profileImageUrlPac;
    private String peerIdDoc;
    private String peerIdPac;
    private String deviceIdDoc;
    private String deviceIdPac;
    private String frontImageUrl;
    private String rearImageUrl;
    private Integer photosChecked;


    public java.util.Date getCreated()
    {
        return created;
    }
    public java.util.Date getUpdated()
    {
        return updated;
    }
    public String getObjectId()
    {
        return objectId;
    }
    public void setObjectId( String objectId ) { this.objectId = objectId; }


    public String getDniDoc()
    {
        return dniDoc;
    }
    public void setDniDoc( String dniDoc ) { this.dniDoc = dniDoc; }

    public String getDniPac()
    {
        return dniPac;
    }
    public void setDniPac( String dniPac ) { this.dniPac = dniPac; }

    public String getNameDoc()
    {
        return nameDoc;
    }
    public void setNameDoc( String nameDoc )
    {
        this.nameDoc = nameDoc;
    }

    public String getNamePac()
    {
        return namePac;
    }
    public void setNamePac( String namePac )
    {
        this.namePac = namePac;
    }

    public String getProfileImageUrlDoc()
    {
        return profileImageUrlDoc;
    }
    public void setProfileImageUrlDoc(String profileImageUrlDoc ) { this.profileImageUrlDoc = profileImageUrlDoc; }

    public String getProfileImageUrlPac()
    {
        return profileImageUrlPac;
    }
    public void setProfileImageUrlPac(String profileImageUrlPac ) { this.profileImageUrlPac = profileImageUrlPac; }

    public String getFrontImageUrl()
    {
        return frontImageUrl;
    }
    public void setFrontImageUrl(String frontImageUrl )
    {
        this.frontImageUrl = frontImageUrl;
    }

    public String getRearImageUrl()
    {
        return rearImageUrl;
    }
    public void setRearImageUrl(String rearImageUrl )
    {
        this.rearImageUrl = rearImageUrl;
    }

    public String getOwnerIdDoc() { return ownerIdDoc; }
    public void setOwnerIdDoc(String ownerIdDoc )
    {
        this.ownerIdDoc = ownerIdDoc;
    }

    public String getOwnerIdPac() { return ownerIdPac; }
    public void setOwnerIdPac(String ownerIdPac )
    {
        this.ownerIdPac = ownerIdPac;
    }

    public String getPeerIdDoc()
    {
        return peerIdDoc;
    }
    public void setPeerIdDoc(String peerIdDoc )
    {
        this.peerIdDoc = peerIdDoc;
    }

    public String getPeerIdPac()
    {
        return peerIdPac;
    }
    public void setPeerIdPac(String peerIdPac )
    {
        this.peerIdPac = peerIdPac;
    }

    public String getDeviceIdDoc()
    {
        return deviceIdDoc;
    }
    public void setDeviceIdDoc(String deviceIdDoc )
    {
        this.deviceIdDoc = deviceIdDoc;
    }

    public String getDeviceIdPac()
    {
        return deviceIdPac;
    }
    public void setDeviceIdPac(String deviceIdPac )
    {
        this.deviceIdPac = deviceIdPac;
    }

    public Integer getPhotosChecked() { return photosChecked; }
    public void setPhotosChecked(Integer photosChecked) { this.photosChecked = photosChecked; }

}