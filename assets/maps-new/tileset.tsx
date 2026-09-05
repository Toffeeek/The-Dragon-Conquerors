<?xml version="1.0" encoding="UTF-8"?>
<tileset version="1.10" tiledversion="1.12.1" name="tileset" tilewidth="16" tileheight="16" tilecount="192" columns="12">
 <image source="tileset.png" width="192" height="256"/>
 <!-- Grass and its decorative shades are walkable; cliff, water and transparent pixels are solid. -->
 <properties>
  <property name="walkableColors" value="3e8948,265c42,3d6c43"/>
 </properties>
 <!-- Terrain type controls fall rules; the generated pixel mask controls the actual collision outline. -->
 <tile id="13"><properties><property name="terrain" value="ground"/></properties></tile>
 <tile id="62"><properties><property name="terrain" value="ground"/></properties></tile>
 <tile id="181"><properties><property name="terrain" value="ground"/></properties></tile>
 <tile id="3"><properties><property name="terrain" value="fall"/></properties></tile>
 <tile id="4"><properties><property name="terrain" value="fall"/></properties></tile>
 <tile id="5"><properties><property name="terrain" value="fall"/></properties></tile>
 <tile id="15"><properties><property name="terrain" value="fall"/></properties></tile>
 <tile id="16"><properties><property name="terrain" value="fall"/></properties></tile>
 <tile id="17"><properties><property name="terrain" value="fall"/></properties></tile>
 <tile id="27"><properties><property name="terrain" value="fall"/></properties></tile>
 <tile id="28"><properties><property name="terrain" value="fall"/></properties></tile>
 <tile id="29"><properties><property name="terrain" value="fall"/></properties></tile>
 <tile id="39"><properties><property name="terrain" value="fall"/></properties></tile>
 <tile id="40"><properties><property name="terrain" value="fall"/></properties></tile>
 <tile id="51"><properties><property name="terrain" value="fall"/></properties></tile>
 <tile id="52"><properties><property name="terrain" value="fall"/></properties></tile>
 <tile id="180"><properties><property name="terrain" value="fall"/></properties></tile>
 <tile id="8">
  <animation>
   <frame tileid="8" duration="150"/>
   <frame tileid="9" duration="150"/>
   <frame tileid="10" duration="150"/>
   <frame tileid="11" duration="150"/>
  </animation>
 </tile>
</tileset>
