/******************************************************************************
 *
 * Copyright (C) 1997-2008 by Dimitri van Heesch.
 *
 * Permission to use, copy, modify, and distribute this software and its
 * documentation under the terms of the GNU General Public License is hereby 
 * granted. No representations are made about the suitability of this software 
 * for any purpose. It is provided "as is" without express or implied warranty.
 * See the GNU General Public License for more details.
 *
 * Documents produced by Doxygen are derivative works derived from the
 * input used in their production; they are not affected by this license.
 *
 */

/******************************************************************************
 * ftvhelp.h,v 1.0 2000/09/06 16:09:00
 *
 * Kenney Wong <kwong@ea.com>
 *
 * Folder Tree View for offline help on browsers that do not support HTML Help.
 */

#pragma once

#include "qtbc.h"
#include <qtextstream.h>
#include <qlist.h>
#include "index.h"
#include "config.h"
#include "doxygen.h"
#include "message.h"
#include "language.h"
#include "htmlgen.h"

class QFile;
struct FTVNode;

struct FTVImageInfo
{
  const char *alt;
  const char *name;
  const unsigned char *data;
  unsigned int len;
  unsigned short width, height;
};

extern FTVImageInfo image_info[];

#define FTVIMG_blank        0
#define FTVIMG_doc          1
#define FTVIMG_folderclosed 2
#define FTVIMG_folderopen   3
#define FTVIMG_lastnode     4
#define FTVIMG_link         5
#define FTVIMG_mlastnode    6
#define FTVIMG_mnode        7
#define FTVIMG_node         8
#define FTVIMG_plastnode    9
#define FTVIMG_pnode       10
#define FTVIMG_vertline    11

#define FTV_S(name) #name
#define FTV_ICON_FILE(name) "ftv2" FTV_S(name) ".png"
#define FTVIMG_INDEX(name) FTVIMG_ ## name
#define FTV_INFO(name) ( image_info[FTVIMG_INDEX(name)] )
#define FTV_IMGATTRIBS(name) \
    "src=\"" FTV_ICON_FILE(name) "\" " \
    "alt=\"" << FTV_INFO(name).alt << "\" " \
    "width=\"" << FTV_INFO(name).width << "\" " \
    "height=\"" << FTV_INFO(name).height << "\" "

/*! A class that generates a dynamic tree view side panel.
 */
class FTVHelp : public IndexIntf
{
  public:
    FTVHelp(bool topLevelIndex = true);
    ~FTVHelp();
    void initialize();
    void finalize();
    void incContentsDepth();
    void decContentsDepth();
    void addContentsItem(bool isDir,
                         const char *name,
                         const char *ref,
                         const char *file,
                         const char *anchor);
    //void addIndexItem(const char *, const char *, 
    //                  const char *, const char *,
    //                  const char *, const MemberDef *) {}
    void addIndexItem(Definition *,MemberDef *,const char *,const char *) {}
    void addIndexFile(const char *) {}
    void addImageFile(const char *) {}
    void addStyleSheetFile(const char *) {}
    void generateTreeView(QString* OutString = NULL)
{
  QCString fileName;
  QFile f;
  static bool searchEngine = Config_getBool("SEARCHENGINE");
  static bool serverBasedSearch = Config_getBool("SERVER_BASED_SEARCH");
  
  generateTreeViewImages();
  
  // If top level index, generate alternative index.html as a frame
  if (m_topLevelIndex)
  {
    fileName=Config_getString("HTML_OUTPUT")+"/index"+Doxygen::htmlFileExtension;
    f.setName(fileName);
    if (!f.open(IO_WriteOnly))
    {
      err("Cannot open file %s for writing!\n",fileName.data());
      return;
    }
    else
    {
      QTextStream t(&f);
#if QT_VERSION >= 200
      t.setEncoding(QTextStream::UnicodeUTF8);
#endif
      //t << "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Frameset//EN\">\n";
      t << "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Frameset//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd\">\n";
      t << "<html xmlns=\"http://www.w3.org/1999/xhtml\">\n<head>\n";
      t << "<meta http-equiv=\"Content-Type\" content=\"text/xhtml;charset=UTF-8\"/>\n";
      t << "<title>"; 
      if (Config_getString("PROJECT_NAME").isEmpty())
      {
        t << "Doxygen Documentation";
      }
      else
      {
        t << Config_getString("PROJECT_NAME");
      }
      t << "</title>\n</head>" << endl;
      t << "<frameset cols=\"" << Config_getInt("TREEVIEW_WIDTH") << ",*\">" << endl;
      t << "  <frame src=\"tree" << Doxygen::htmlFileExtension << "\" name=\"treefrm\"/>" << endl;
      t << "  <frame src=\"main" << Doxygen::htmlFileExtension << "\" name=\"basefrm\"/>" << endl;
      t << "  <noframes>" << endl;
      t << "    <body>" << endl;
      t << "    <a href=\"main" << Doxygen::htmlFileExtension << "\">Frames are disabled. Click here to go to the main page.</a>" << endl;
      t << "    </body>" << endl;
      t << "  </noframes>" << endl;
      t << "</frameset>" << endl;
      t << "</html>" << endl;
      f.close();
    }
  }

  // Generate tree view
  if (!OutString)
    OutString = new QString;
  QTextOStream t(OutString);
  t.setEncoding(QTextStream::UnicodeUTF8);

  if (m_topLevelIndex)
  {
    if (searchEngine)
    {
      t << "<!-- This comment will put IE 6, 7 and 8 in quirks mode -->" << endl;
    }
    t << "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n";
    t << "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\" lang=\"en\">\n";
    t << "  <head>\n";
    t << "    <meta http-equiv=\"Content-Type\" content=\"text/xhtml;charset=UTF-8\"/>\n";
    t << "    <meta http-equiv=\"Content-Style-Type\" content=\"text/css\" />\n";
    t << "    <meta http-equiv=\"Content-Language\" content=\"en\" />\n";
    if (searchEngine)
    {
      t << "    <link href=\"search/search.css\" rel=\"stylesheet\" type=\"text/css\"/>" << endl;
      t << "    <script type=\"text/javaScript\" src=\"search/search.js\"></script>" << endl;
    }
    t << "    <link rel=\"stylesheet\" href=\"";
    QCString cssname=Config_getString("HTML_STYLESHEET");
    if (cssname.isEmpty())
    {
      t << "doxygen.css";
    }
    else
    {
      QFileInfo cssfi(cssname);
      if (!cssfi.exists())
      {
        err("Error: user specified HTML style sheet file does not exist!\n");
      }
      t << cssfi.fileName();
    }
    t << "\"/>" << endl;
    t << "    <title>TreeView</title>\n";
  }
  t << "    <script type=\"text/javascript\">\n";
  t << "    <!-- // Hide script from old browsers\n";
  t << "    \n";

  /* User has clicked on a node (folder or +/-) in the tree */
  t << "    function toggleFolder(id, imageNode) \n";
  t << "    {\n";
  t << "      var folder = document.getElementById(id);\n";
  t << "      var l = imageNode.src.length;\n";
  /* If the user clicks on the book icon, we move left one image so 
   * the code (below) will also adjust the '+' icon. 
   */
  t << "      if (imageNode.src.substring(l-20,l)==\"" FTV_ICON_FILE(folderclosed) "\" || \n";
  t << "          imageNode.src.substring(l-18,l)==\"" FTV_ICON_FILE(folderopen)  "\")\n";
  t << "      {\n";
  t << "        imageNode = imageNode.previousSibling;\n";
  t << "        l = imageNode.src.length;\n";
  t << "      }\n";
  t << "      if (folder == null) \n";
  t << "      {\n";
  t << "      } \n";
  /* Node controls a open section, we need to close it */
  t << "      else if (folder.style.display == \"block\") \n";
  t << "      {\n";
  t << "        if (imageNode != null) \n";
  t << "        {\n";
  t << "          imageNode.nextSibling.src = \"" FTV_ICON_FILE(folderclosed) "\";\n";
  t << "          if (imageNode.src.substring(l-13,l) == \"" FTV_ICON_FILE(mnode) "\")\n";
  t << "          {\n";
  t << "            imageNode.src = \"" FTV_ICON_FILE(pnode) "\";\n";
  t << "          }\n";
  t << "          else if (imageNode.src.substring(l-17,l) == \"" FTV_ICON_FILE(mlastnode) "\")\n";
  t << "          {\n";
  t << "            imageNode.src = \"" FTV_ICON_FILE(plastnode) "\";\n";
  t << "          }\n";
  t << "        }\n";
  t << "        folder.style.display = \"none\";\n";
  t << "      } \n";
  t << "      else \n"; /* section is closed, we need to open it */
  t << "      {\n";
  t << "        if (imageNode != null) \n";
  t << "        {\n";
  t << "          imageNode.nextSibling.src = \"" FTV_ICON_FILE(folderopen) "\";\n";
  t << "          if (imageNode.src.substring(l-13,l) == \"" FTV_ICON_FILE(pnode) "\")\n";
  t << "          {\n";
  t << "            imageNode.src = \"" FTV_ICON_FILE(mnode) "\";\n";
  t << "          }\n";
  t << "          else if (imageNode.src.substring(l-17,l) == \"" FTV_ICON_FILE(plastnode) "\")\n";
  t << "          {\n";
  t << "            imageNode.src = \"" FTV_ICON_FILE(mlastnode) "\";\n";
  t << "          }\n";
  t << "        }\n";
  t << "        folder.style.display = \"block\";\n";
  t << "      }\n";
  t << "    }\n";
  t << "\n";
  t << "    // End script hiding -->        \n";
  t << "    </script>\n";
  if (m_topLevelIndex)
  {
    t << "  </head>\n";
    t << "\n";
    t << "  <body class=\"ftvtree\"";
    if (searchEngine && !serverBasedSearch)
    {
      t << " onload='searchBox.OnSelectItem(0);'";
    }
    t << ">\n";
    if (searchEngine)
    {
      t << "      <script type=\"text/javascript\"><!--\n";
      t << "      var searchBox = new SearchBox(\"searchBox\", \"search\", true, '" 
        << theTranslator->trSearch() << "');\n";
      t << "      --></script>\n";
      if (!serverBasedSearch)
      {
        t << "      <div id=\"MSearchBox\" class=\"MSearchBoxInactive\">\n";
        t << "      <div class=\"MSearchBoxRow\"><span class=\"MSearchBoxLeft\">\n";
        t << "      <a id=\"MSearchClose\" href=\"javascript:searchBox.CloseResultsWindow()\">"
          << "<img id=\"MSearchCloseImg\" border=\"0\" src=\"search/close.png\" alt=\"\"/></a>\n";
        t << "      <input type=\"text\" id=\"MSearchField\" value=\"" 
          << theTranslator->trSearch() << "\" accesskey=\"S\"\n";
        t << "           onfocus=\"searchBox.OnSearchFieldFocus(true)\" \n";
        t << "           onblur=\"searchBox.OnSearchFieldFocus(false)\" \n";
        t << "           onkeyup=\"searchBox.OnSearchFieldChange(event)\"/>\n";
        t << "      </span><span class=\"MSearchBoxRight\">\n";
        t << "      <img id=\"MSearchSelect\" src=\"search/search.png\"\n";
        t << "           onmouseover=\"return searchBox.OnSearchSelectShow()\"\n";
        t << "           onmouseout=\"return searchBox.OnSearchSelectHide()\"\n";
        t << "           alt=\"\"/>\n";
        t << "      </span></div><div class=\"MSearchBoxSpacer\">&nbsp;</div>\n";
        t << "      </div>\n";
        HtmlGenerator::writeSearchFooter(t,QCString());
      }
      else
      {
        t << "        <div id=\"MSearchBox\" class=\"MSearchBoxInactive\">\n";
        t << "            <form id=\"FSearchBox\" action=\"search.php\" method=\"get\" target=\"basefrm\">\n";
        t << "              <img id=\"MSearchSelect\" src=\"search/search.png\" alt=\"\"/>\n";
        t << "              <input type=\"text\" id=\"MSearchField\" name=\"query\" value=\""
          << theTranslator->trSearch() << "\" size=\"20\" accesskey=\"S\" \n";
        t << "                     onfocus=\"searchBox.OnSearchFieldFocus(true)\" \n";
        t << "                     onblur=\"searchBox.OnSearchFieldFocus(false)\"/>\n";
        t << "            </form>\n";
        t << "          <div class=\"MSearchBoxSpacer\">&nbsp;</div>\n";
        t << "        </div>\n";
      }
    }
    t << "    <div class=\"directory\">\n";
    t << "      <h3 class=\"swap\"><span>";
    QCString &projName = Config_getString("PROJECT_NAME");
    if (projName.isEmpty())
    {
      t << "Root";
    }
    else
    {
      t << projName;
    }
    t << "</span></h3>\n";
  }
  else
  {
    t << "    <div class=\"directory-alt\">\n";
    t << "      <br/>\n";
  }
  t << "      <div style=\"display: block;\">\n";

  generateTree(t,m_indentNodes[0],0);

  t << "      </div>\n";
  t << "    </div>\n";
  
  if (m_topLevelIndex)
  {
    t << "  </body>\n";
    t << "</html>\n";
  }
  
  if (m_topLevelIndex)
  {
    fileName=Config_getString("HTML_OUTPUT")+"/tree"+Doxygen::htmlFileExtension;
    f.setName(fileName);
    if (!f.open(IO_WriteOnly))
    {
      err("Cannot open file %s for writing!\n",fileName.data());
      return;
    }
    else
    {
      QTextStream t(&f);
      t.setEncoding(QTextStream::UnicodeUTF8);
      t << *OutString << endl;
      f.close();
    }
  }
}
  private:
    void generateTreeViewImages();
    void generateTree(QTextStream &t,const QList<FTVNode> &nl,int level);
    void generateIndent(QTextStream &t,FTVNode *n,int level);
    void generateLink(QTextStream &t,FTVNode *n);
    QList<FTVNode> *m_indentNodes;
    int m_indent;
    bool m_topLevelIndex;
};

