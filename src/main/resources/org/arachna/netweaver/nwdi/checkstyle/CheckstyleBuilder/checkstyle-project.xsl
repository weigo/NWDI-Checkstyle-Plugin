<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">

  <xsl:output method="xml" version="1.0" indent="yes" encoding="UTF-8" />
  <xsl:param name="reportBase" />
  <xsl:param name="checkStyleConfig" />
  <!-- generate ant project -->
  <xsl:template match="/">
    <xsl:element name="project">
      <xsl:attribute name="name">p</xsl:attribute>
      <xsl:attribute name="default">all</xsl:attribute>
      <xsl:apply-templates />
    </xsl:element>
  </xsl:template>

  <xsl:template match="paths/path">
    <xsl:element name="path">
      <xsl:attribute name="id">
    <xsl:value-of select="@id" />
    </xsl:attribute>
      <xsl:element name="fileset">
        <xsl:attribute name="dir">
           <xsl:value-of select="@location" />
        </xsl:attribute>
        <include name="*.jar" />
      </xsl:element>
    </xsl:element>
  </xsl:template>

  <xsl:template match="targets/checkstyle">
    <xsl:variable name="reportDir">
      <xsl:value-of select="concat($reportBase, '/', @name)" />
    </xsl:variable>
    <xsl:element name="target">
      <xsl:attribute name="name">
        <xsl:value-of select="@name" />
      </xsl:attribute>
      <xsl:element name="mkdir">
        <xsl:attribute name="dir">
          <xsl:value-of select="$reportDir" />
        </xsl:attribute>
      </xsl:element>
      <xsl:element name="checkstyle">
        <xsl:attribute name="failOnError">false</xsl:attribute>
        <xsl:attribute name="config">
          <xsl:value-of select="$checkStyleConfig" />
        </xsl:attribute>
        <xsl:apply-templates />
        <xsl:element name="formatter">
          <xsl:attribute name="type">xml</xsl:attribute>
          <xsl:attribute name="toFile">
        <xsl:value-of select="concat($reportDir, '/checkstyle_errors.xml')"></xsl:value-of>
        </xsl:attribute>
        </xsl:element>
      </xsl:element>
    </xsl:element>
  </xsl:template>

  <xsl:template match="sources/src">
    <xsl:element name="fileset">
      <xsl:attribute name="dir">
           <xsl:value-of select="@folder" />
        </xsl:attribute>
      <include name="**/*.java" />
    </xsl:element>
  </xsl:template>
</xsl:stylesheet>