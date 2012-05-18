package org.zanata.webtrans.client.ui;

import java.io.Serializable;

public interface HasManageUserPanel extends Serializable
{
   void updateSessionLabel(String session);

   void addColor(String color);

   void setColor(String color);

   void clearColorList();
}
