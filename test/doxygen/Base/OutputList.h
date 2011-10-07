/******************************************************************************
 *
 * $Id: outputlist.h,v 1.47 2001/03/19 19:27:41 root Exp $
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

#pragma once

#include "qtbc.h"
#include <qlist.h>
#include "index.h" // for IndexSections
#include "outputgen.h"

#include "config.h"
#include "message.h"
#include "definition.h"

#include "docparser.h"


// one argument
#define FORALL1(a1,p1)                                        \
	void OutputList::forall(void (OutputGenerator::*func)(a1),a1) \
{                                                             \
	OutputGenerator *og=outputs->first();                       \
	while (og)                                                  \
  {                                                           \
  if (og->isEnabled()) (og->*func)(p1);                     \
  og=outputs->next();                                       \
  }                                                           \
}                     

// two arguments
#define FORALL2(a1,a2,p1,p2)                                        \
	void OutputList::forall(void (OutputGenerator::*func)(a1,a2),a1,a2) \
{                                                                   \
	OutputGenerator *og=outputs->first();                             \
	while (og)                                                        \
  {                                                                 \
  if (og->isEnabled()) (og->*func)(p1,p2);                        \
  og=outputs->next();                                             \
  }                                                                 \
}                     

// three arguments
#define FORALL3(a1,a2,a3,p1,p2,p3)                                        \
	void OutputList::forall(void (OutputGenerator::*func)(a1,a2,a3),a1,a2,a3) \
{                                                                         \
	OutputGenerator *og=outputs->first();                                   \
	while (og)                                                              \
  {                                                                       \
  if (og->isEnabled()) (og->*func)(p1,p2,p3);                           \
  og=outputs->next();                                                   \
  }                                                                       \
}                     

// four arguments
#define FORALL4(a1,a2,a3,a4,p1,p2,p3,p4)                                        \
	void OutputList::forall(void (OutputGenerator::*func)(a1,a2,a3,a4),a1,a2,a3,a4) \
{                                                                               \
	OutputGenerator *og=outputs->first();                                         \
	while (og)                                                                    \
  {                                                                             \
  if (og->isEnabled()) (og->*func)(p1,p2,p3,p4);                              \
  og=outputs->next();                                                         \
  }                                                                             \
}                     

// five arguments
#define FORALL5(a1,a2,a3,a4,a5,p1,p2,p3,p4,p5)                                        \
	void OutputList::forall(void (OutputGenerator::*func)(a1,a2,a3,a4,a5),a1,a2,a3,a4,a5) \
{                                                                                     \
	OutputGenerator *og=outputs->first();                                               \
	while (og)                                                                          \
  {                                                                                   \
  if (og->isEnabled()) (og->*func)(p1,p2,p3,p4,p5);                                 \
  og=outputs->next();                                                               \
  }                                                                                   \
}                     
  
class ClassDiagram;
class DotClassGraph;
class DotDirDeps;
class DotInclDepGraph;
class DotGfxHierarchyTable;
class SectionDict;
class DotGroupCollaboration;

class OutputList : public OutputDocInterface
{
  public:
	  OutputList(bool)
	  {
		  //printf("OutputList::OutputList()\n");
		  outputs = new QList<OutputGenerator>;
		  outputs->setAutoDelete(TRUE);
	  }

	  virtual ~OutputList()
	  {
		  //printf("OutputList::~OutputList()\n");
		  delete outputs;
	  }

	  void add(const OutputGenerator *og)
	  {
		  if (og) outputs->append(og);
	  }
    
	  void disableAllBut(OutputGenerator::OutputType o)
	  {
		  OutputGenerator *og=outputs->first();
		  while (og)
		  {
			  og->disableIfNot(o);
			  og=outputs->next();
		  }
	  }
	  void enableAll()
	  {
		  OutputGenerator *og=outputs->first();
		  while (og)
		  {
			  og->enable();
			  og=outputs->next();
		  }
	  }
	  void disableAll()
	  {
		  OutputGenerator *og=outputs->first();
		  while (og)
		  {
			  og->disable();
			  og=outputs->next();
		  }
	  }
	  void disable(OutputGenerator::OutputType o)
	  {
		  OutputGenerator *og=outputs->first();
		  while (og)
		  {
			  og->disableIf(o);
			  og=outputs->next();
		  }
	  }
	  void enable(OutputGenerator::OutputType o)
	  {
		  OutputGenerator *og=outputs->first();
		  while (og)
		  {
			  og->enableIf(o);
			  og=outputs->next();
		  }
	  }

	  bool isEnabled(OutputGenerator::OutputType o)
	  {
		  bool result=FALSE;
		  OutputGenerator *og=outputs->first();
		  while (og)
		  {
			  result=result || og->isEnabled(o);
			  og=outputs->next();
		  }
		  return result;
	  }
	  void pushGeneratorState()
	  {
		  OutputGenerator *og=outputs->first();
		  while (og)
		  {
			  og->pushGeneratorState();
			  og=outputs->next();
		  }
	  }
	  void popGeneratorState()
	  {
		  OutputGenerator *og=outputs->first();
		  while (og)
		  {
			  og->popGeneratorState();
			  og=outputs->next();
		  }
	  }



    //////////////////////////////////////////////////
    // OutputDocInterface implementation
    //////////////////////////////////////////////////

    void parseDoc(const char *fileName,int startLine,
                  Definition *ctx,MemberDef *md,const QCString &docStr,
                  bool indexWords,bool isExample,const char *exampleName=0,
				  bool singleLine=FALSE,bool linkFromIndex=FALSE)
	{
		int count=0;
		if (docStr.isEmpty()) return;

		OutputGenerator *og=outputs->first();
		while (og)
		{
			if (og->isEnabled()) count++;
			og=outputs->next();
		}
		if (count==0) return; // no output formats enabled.

		DocNode *root=0;
		if (docStr.at(docStr.length()-1)=='\n')
		{
			root = validatingParseDoc(fileName,startLine,
				ctx,md,docStr,indexWords,isExample,exampleName,
				singleLine,linkFromIndex);
		}
		else
		{
			root = validatingParseDoc(fileName,startLine,
				ctx,md,docStr+"\n",indexWords,isExample,exampleName,
				singleLine,linkFromIndex);
		}

		og=outputs->first();
		while (og)
		{
			//printf("og->printDoc(extension=%s)\n",
			//    ctx?ctx->getDefFileExtension().data():"<null>");
			if (og->isEnabled()) og->printDoc(root,ctx?ctx->getDefFileExtension():QCString(""));
			og=outputs->next();
		}

		delete root;
	}
	void parseText(const QCString &textStr)
	{
		int count=0;
		OutputGenerator *og=outputs->first();
		while (og)
		{
			if (og->isEnabled()) count++;
			og=outputs->next();
		}
		if (count==0) return; // no output formats enabled.

		DocNode *root = validatingParseText(textStr);

		og=outputs->first();
		while (og)
		{
			if (og->isEnabled()) og->printDoc(root,0);
			og=outputs->next();
		}

		delete root;
	}
    

    void startIndexSection(IndexSections is)
    { forall(&OutputGenerator::startIndexSection,is); }
    void endIndexSection(IndexSections is)
    { forall(&OutputGenerator::endIndexSection,is); }
    void writePageLink(const char *name,bool first)
    { forall(&OutputGenerator::writePageLink,name,first); }
    void startProjectNumber()
    { forall(&OutputGenerator::startProjectNumber); }
    void endProjectNumber()
    { forall(&OutputGenerator::endProjectNumber); }
    void writeStyleInfo(int part) 
    { forall(&OutputGenerator::writeStyleInfo,part); }
    void startFile(const char *name,const char *manName,const char *title)
    { forall(&OutputGenerator::startFile,name,manName,title); }
    void writeFooter()
    { forall(&OutputGenerator::writeFooter); }
    void endFile() 
    { forall(&OutputGenerator::endFile); }
    void startTitleHead(const char *fileName) 
    { forall(&OutputGenerator::startTitleHead,fileName); }
    void endTitleHead(const char *fileName,const char *name)
    { forall(&OutputGenerator::endTitleHead,fileName,name); }
    void startTitle() 
    { forall(&OutputGenerator::startTitle); }
    void endTitle() 
    { forall(&OutputGenerator::endTitle); }
    //void newParagraph() 
    //{ forall(&OutputGenerator::newParagraph); }
    void startParagraph() 
    { forall(&OutputGenerator::startParagraph); }
    void endParagraph() 
    { forall(&OutputGenerator::endParagraph); }
    void writeString(const char *text) 
    { forall(&OutputGenerator::writeString,text); }
    void startIndexListItem() 
    { forall(&OutputGenerator::startIndexListItem); }
    void endIndexListItem() 
    { forall(&OutputGenerator::endIndexListItem); }
    void startIndexList() 
    { forall(&OutputGenerator::startIndexList); }
    void endIndexList() 
    { forall(&OutputGenerator::endIndexList); }
    void startIndexKey()
    { forall(&OutputGenerator::startIndexKey); }
    void endIndexKey()
    { forall(&OutputGenerator::endIndexKey); }
    void startIndexValue(bool b)
    { forall(&OutputGenerator::startIndexValue,b); }
    void endIndexValue(const char *name,bool b)
    { forall(&OutputGenerator::endIndexValue,name,b); }
    void startItemList() 
    { forall(&OutputGenerator::startItemList); }
    void endItemList() 
    { forall(&OutputGenerator::endItemList); }
    void startIndexItem(const char *ref,const char *file)
    { forall(&OutputGenerator::startIndexItem,ref,file); }
    void endIndexItem(const char *ref,const char *file)
    { forall(&OutputGenerator::endIndexItem,ref,file); }
    void docify(const char *s)
    { forall(&OutputGenerator::docify,s); }
    void codify(const char *s)
    { forall(&OutputGenerator::codify,s); }
    void writeObjectLink(const char *ref,const char *file,
                         const char *anchor, const char *name)
    { forall(&OutputGenerator::writeObjectLink,ref,file,anchor,name); }
    void writeCodeLink(const char *ref,const char *file,
                       const char *anchor,const char *name,
                       const char *tooltip)
    { forall(&OutputGenerator::writeCodeLink,ref,file,anchor,name,tooltip); }
    void startTextLink(const char *file,const char *anchor)
    { forall(&OutputGenerator::startTextLink,file,anchor); }
    void endTextLink()
    { forall(&OutputGenerator::endTextLink); }
    void startHtmlLink(const char *url)
    { forall(&OutputGenerator::startHtmlLink,url); }
    void endHtmlLink()
    { forall(&OutputGenerator::endHtmlLink); }
    void writeStartAnnoItem(const char *type,const char *file, 
                            const char *path,const char *name)
    { forall(&OutputGenerator::writeStartAnnoItem,type,file,path,name); }
    void writeEndAnnoItem(const char *name)
    { forall(&OutputGenerator::writeEndAnnoItem,name); }
    void startTypewriter() 
    { forall(&OutputGenerator::startTypewriter); }
    void endTypewriter() 
    { forall(&OutputGenerator::endTypewriter); }
    void startGroupHeader()
    { forall(&OutputGenerator::startGroupHeader); }
    void endGroupHeader()
    { forall(&OutputGenerator::endGroupHeader); }
    //void writeListItem() 
    //{ forall(&OutputGenerator::writeListItem); }
    void startItemListItem() 
    { forall(&OutputGenerator::startItemListItem); }
    void endItemListItem() 
    { forall(&OutputGenerator::endItemListItem); }
    void startMemberSections()
    { forall(&OutputGenerator::startMemberSections); }
    void endMemberSections()
    { forall(&OutputGenerator::endMemberSections); }
    void startMemberHeader()
    { forall(&OutputGenerator::startMemberHeader); }
    void endMemberHeader()
    { forall(&OutputGenerator::endMemberHeader); }
    void startMemberSubtitle()
    { forall(&OutputGenerator::startMemberSubtitle); }
    void endMemberSubtitle()
    { forall(&OutputGenerator::endMemberSubtitle); }
    void startMemberDocList() 
    { forall(&OutputGenerator::startMemberDocList); }
    void endMemberDocList() 
    { forall(&OutputGenerator::endMemberDocList); }
    void startMemberList() 
    { forall(&OutputGenerator::startMemberList); }
    void endMemberList() 
    { forall(&OutputGenerator::endMemberList); }
    void startAnonTypeScope(int i1) 
    { forall(&OutputGenerator::startAnonTypeScope,i1); }
    void endAnonTypeScope(int i1) 
    { forall(&OutputGenerator::endAnonTypeScope,i1); }
    void startMemberItem(int i1) 
    { forall(&OutputGenerator::startMemberItem,i1); }
    void endMemberItem() 
    { forall(&OutputGenerator::endMemberItem); }
    void startMemberTemplateParams() 
    { forall(&OutputGenerator::startMemberTemplateParams); }
    void endMemberTemplateParams() 
    { forall(&OutputGenerator::endMemberTemplateParams); }
    void startMemberGroupHeader(bool b) 
    { forall(&OutputGenerator::startMemberGroupHeader,b); }
    void endMemberGroupHeader()
    { forall(&OutputGenerator::endMemberGroupHeader); }
    void startMemberGroupDocs()
    { forall(&OutputGenerator::startMemberGroupDocs); }
    void endMemberGroupDocs()
    { forall(&OutputGenerator::endMemberGroupDocs); }
    void startMemberGroup()
    { forall(&OutputGenerator::startMemberGroup); }
    void endMemberGroup(bool last)
    { forall(&OutputGenerator::endMemberGroup,last); }
    void insertMemberAlign(bool templ=FALSE) 
    { forall(&OutputGenerator::insertMemberAlign,templ); }
    void writeRuler() 
    { forall(&OutputGenerator::writeRuler); }
    void writeAnchor(const char *fileName,const char *name)
    { forall(&OutputGenerator::writeAnchor,fileName,name); }
    void startCodeFragment() 
    { forall(&OutputGenerator::startCodeFragment); }
    void endCodeFragment() 
    { forall(&OutputGenerator::endCodeFragment); }
    void startCodeLine() 
    { forall(&OutputGenerator::startCodeLine); }
    void endCodeLine() 
    { forall(&OutputGenerator::endCodeLine); }
    void writeLineNumber(const char *ref,const char *file,const char *anchor,
                         int lineNumber) 
    { forall(&OutputGenerator::writeLineNumber,ref,file,anchor,lineNumber); }
    void startEmphasis() 
    { forall(&OutputGenerator::startEmphasis); }
    void endEmphasis() 
    { forall(&OutputGenerator::endEmphasis); }
    void writeChar(char c)
    { forall(&OutputGenerator::writeChar,c); }
    void startMemberDoc(const char *clName,const char *memName,
                        const char *anchor,const char *title)
    { forall(&OutputGenerator::startMemberDoc,clName,memName,anchor,title); }
    void endMemberDoc(bool hasArgs) 
    { forall(&OutputGenerator::endMemberDoc,hasArgs); }
    void startDoxyAnchor(const char *fName,const char *manName,
                         const char *anchor, const char *name,
                         const char *args)
    { forall(&OutputGenerator::startDoxyAnchor,fName,manName,anchor,name,args); }
    void endDoxyAnchor(const char *fn,const char *anchor)
    { forall(&OutputGenerator::endDoxyAnchor,fn,anchor); }
    void startCodeAnchor(const char *label)
    { forall(&OutputGenerator::startCodeAnchor,label); }
    void endCodeAnchor()
    { forall(&OutputGenerator::endCodeAnchor); }
    void writeLatexSpacing() 
    { forall(&OutputGenerator::writeLatexSpacing); }
    void startDescription() 
    { forall(&OutputGenerator::startDescription); }
    void endDescription() 
    { forall(&OutputGenerator::endDescription); }
    void startDescItem() 
    { forall(&OutputGenerator::startDescItem); }
    void endDescItem() 
    { forall(&OutputGenerator::endDescItem); }
    void startDescForItem() 
    { forall(&OutputGenerator::startDescForItem); }
    void endDescForItem() 
    { forall(&OutputGenerator::endDescForItem); }
    void startSubsection() 
    { forall(&OutputGenerator::startSubsection); }
    void endSubsection() 
    { forall(&OutputGenerator::endSubsection); }
    void startSubsubsection() 
    { forall(&OutputGenerator::startSubsubsection); }
    void endSubsubsection() 
    { forall(&OutputGenerator::endSubsubsection); }
    void startCenter() 
    { forall(&OutputGenerator::startCenter); }
    void endCenter() 
    { forall(&OutputGenerator::endCenter); }
    void startSmall() 
    { forall(&OutputGenerator::startSmall); }
    void endSmall() 
    { forall(&OutputGenerator::endSmall); }
    void lineBreak(const char *style=0) 
    { forall(&OutputGenerator::lineBreak,style); }
    void startBold() 
    { forall(&OutputGenerator::startBold); }
    void endBold() 
    { forall(&OutputGenerator::endBold); }
    void startMemberDescription() 
    { forall(&OutputGenerator::startMemberDescription); }
    void endMemberDescription() 
    { forall(&OutputGenerator::endMemberDescription); }
    void startSimpleSect(SectionTypes t,const char *file,const char *anchor,
                         const char *title) 
    { forall(&OutputGenerator::startSimpleSect,t,file,anchor,title); }
    void endSimpleSect() 
    { forall(&OutputGenerator::endSimpleSect); }
    void startParamList(ParamListTypes t,const char *title) 
    { forall(&OutputGenerator::startParamList,t,title); }
    void endParamList() 
    { forall(&OutputGenerator::endParamList); }
    //void writeDescItem() 
    //{ forall(&OutputGenerator::writeDescItem); }
    void startIndent() 
    { forall(&OutputGenerator::startIndent); }
    void endIndent() 
    { forall(&OutputGenerator::endIndent); }
    void startSection(const char *lab,const char *title,SectionInfo::SectionType t)
    { forall(&OutputGenerator::startSection,lab,title,t); }
    void endSection(const char *lab,SectionInfo::SectionType t)
    { forall(&OutputGenerator::endSection,lab,t); }
    void addIndexItem(const char *s1,const char *s2)
    { forall(&OutputGenerator::addIndexItem,s1,s2); }
    void writeSynopsis() 
    { forall(&OutputGenerator::writeSynopsis); }
    void startClassDiagram()
    { forall(&OutputGenerator::startClassDiagram); }
    void endClassDiagram(const ClassDiagram &d,const char *f,const char *n)
    { forall(&OutputGenerator::endClassDiagram,d,f,n); }
    void startPageRef()
    { forall(&OutputGenerator::startPageRef); }
    void endPageRef(const char *c,const char *a)
    { forall(&OutputGenerator::endPageRef,c,a); }
    void startQuickIndices()
    { forall(&OutputGenerator::startQuickIndices); }
    void endQuickIndices()
    { forall(&OutputGenerator::endQuickIndices); }
    void writeQuickLinks(bool compact,HighlightedItem hli)
    { forall(&OutputGenerator::writeQuickLinks,compact,hli); }
    void startContents()
    { forall(&OutputGenerator::startContents); }
    void endContents()
    { forall(&OutputGenerator::endContents); }
    void writeNonBreakableSpace(int num)
    { forall(&OutputGenerator::writeNonBreakableSpace,num); }
    void startDescTable()
    { forall(&OutputGenerator::startDescTable); }
    void endDescTable()
    { forall(&OutputGenerator::endDescTable); }
    void startDescTableTitle()
    { forall(&OutputGenerator::startDescTableTitle); }
    void endDescTableTitle()
    { forall(&OutputGenerator::endDescTableTitle); }
    void startDescTableData()
    { forall(&OutputGenerator::startDescTableData); }
    void endDescTableData()
    { forall(&OutputGenerator::endDescTableData); }
    void startDotGraph()
    { forall(&OutputGenerator::startDotGraph); }
    void endDotGraph(const DotClassGraph &g)
    { forall(&OutputGenerator::endDotGraph,g); }
    void startInclDepGraph()
    { forall(&OutputGenerator::startInclDepGraph); }
    void endInclDepGraph(const DotInclDepGraph &g)
    { forall(&OutputGenerator::endInclDepGraph,g); }
    void startCallGraph()
    { forall(&OutputGenerator::startCallGraph); }
    void endCallGraph(const DotCallGraph &g)
    { forall(&OutputGenerator::endCallGraph,g); }
    void startDirDepGraph()
    { forall(&OutputGenerator::startDirDepGraph); }
    void endDirDepGraph(const DotDirDeps &g)
    { forall(&OutputGenerator::endDirDepGraph,g); }
    void startGroupCollaboration()
    { forall(&OutputGenerator::startGroupCollaboration); }
    void endGroupCollaboration(const DotGroupCollaboration &g)
    { forall(&OutputGenerator::endGroupCollaboration,g); }
    void writeGraphicalHierarchy(const DotGfxHierarchyTable &g)
    { forall(&OutputGenerator::writeGraphicalHierarchy,g); }
    void startTextBlock(bool dense=FALSE)
    { forall(&OutputGenerator::startTextBlock,dense); }
    void endTextBlock(bool paraBreak=FALSE)
    { forall(&OutputGenerator::endTextBlock,paraBreak); }
    void lastIndexPage()
    { forall(&OutputGenerator::lastIndexPage); }
    void startMemberDocPrefixItem()
    { forall(&OutputGenerator::startMemberDocPrefixItem); }
    void endMemberDocPrefixItem()
    { forall(&OutputGenerator::endMemberDocPrefixItem); }
    void startMemberDocName(bool align)
    { forall(&OutputGenerator::startMemberDocName,align); }
    void endMemberDocName()
    { forall(&OutputGenerator::endMemberDocName); }
    void startParameterType(bool first,const char *key)
    { forall(&OutputGenerator::startParameterType,first,key); }
    void endParameterType()
    { forall(&OutputGenerator::endParameterType); }
    void startParameterName(bool one)
    { forall(&OutputGenerator::startParameterName,one); }
    void endParameterName(bool last,bool one,bool bracket)
    { forall(&OutputGenerator::endParameterName,last,one,bracket); }
    void startParameterList(bool openBracket)
    { forall(&OutputGenerator::startParameterList,openBracket); }
    void endParameterList()
    { forall(&OutputGenerator::endParameterList); }

    void startConstraintList(const char *header) 
    { forall(&OutputGenerator::startConstraintList,header); }
    void startConstraintParam() 
    { forall(&OutputGenerator::startConstraintParam); }
    void endConstraintParam() 
    { forall(&OutputGenerator::endConstraintParam); }
    void startConstraintType()
    { forall(&OutputGenerator::startConstraintType); }
    void endConstraintType()
    { forall(&OutputGenerator::endConstraintType); }
    void startConstraintDocs()
    { forall(&OutputGenerator::startConstraintDocs); }
    void endConstraintDocs()
    { forall(&OutputGenerator::endConstraintDocs); }
    void endConstraintList()
    { forall(&OutputGenerator::endConstraintList); }
    void startFontClass(const char *c)
    { forall(&OutputGenerator::startFontClass,c); }
    void endFontClass()
    { forall(&OutputGenerator::endFontClass); }
    void writeCodeAnchor(const char *name)
    { forall(&OutputGenerator::writeCodeAnchor,name); }
    void startPlainFile(const char *name)
    { 
      OutputGenerator *og=outputs->first();
      while (og)
      {
        if (og->isEnabled()) (og->startPlainFile)(name);
        og=outputs->next();
      }
    }
    void endPlainFile() 
    { 
      OutputGenerator *og=outputs->first();
      while (og)
      {
        if (og->isEnabled()) (og->endPlainFile)();
        og=outputs->next();
      }
    }
    void linkableSymbol(int,const char *,Definition *,Definition *) {}



  private:
    void debug();
    void clear();
    
	void forall(void (OutputGenerator::*func)())
	{
		OutputGenerator *og=outputs->first();
		while (og)
		{
			if (og->isEnabled()) (og->*func)();
			og=outputs->next();
		}
	}
	FORALL1(const char *a1,a1)
	FORALL1(char a1,a1)
	FORALL1(int a1,a1)
	FORALL1(const DotClassGraph &a1,a1)
	FORALL1(const DotInclDepGraph &a1,a1)
	FORALL1(const DotCallGraph &a1,a1)
	FORALL1(const DotDirDeps &a1,a1)
	FORALL1(const DotGfxHierarchyTable &a1,a1)
	FORALL1(const DotGroupCollaboration &a1,a1)
	FORALL1(SectionTypes a1,a1)
#if defined(HAS_BOOL_TYPE) || defined(Q_HAS_BOOL_TYPE)
	FORALL1(bool a1,a1)
	FORALL2(bool a1,int a2,a1,a2)
	FORALL2(bool a1,bool a2,a1,a2)
	FORALL4(const char *a1,const char *a2,const char *a3,bool a4,a1,a2,a3,a4)
#endif
	FORALL2(int a1,bool a2,a1,a2)
	FORALL2(bool a1,HighlightedItem a2,a1,a2)
	FORALL2(bool a1,const char *a2,a1,a2)
	FORALL2(ParamListTypes a1,const char *a2,a1,a2)
	FORALL1(IndexSections a1,a1)
	FORALL2(const char *a1,const char *a2,a1,a2)
	FORALL2(const char *a1,bool a2,a1,a2)
	FORALL2(const char *a1,SectionInfo::SectionType a2,a1,a2)
	FORALL3(bool a1,bool a2,bool a3,a1,a2,a3)
	FORALL3(const ClassDiagram &a1,const char *a2,const char *a3,a1,a2,a3)
	FORALL3(const char *a1,const char *a2,const char *a3,a1,a2,a3)
	FORALL3(const char *a1,const char *a2,bool a3,a1,a2,a3)
	FORALL3(const char *a1,const char *a2,SectionInfo::SectionType a3,a1,a2,a3)
	FORALL3(uchar a1,uchar a2,uchar a3,a1,a2,a3)
	FORALL4(SectionTypes a1,const char *a2,const char *a3,const char *a4,a1,a2,a3,a4)
	FORALL4(const char *a1,const char *a2,const char *a3,const char *a4,a1,a2,a3,a4)
	FORALL4(const char *a1,const char *a2,const char *a3,int a4,a1,a2,a3,a4)
	FORALL5(const char *a1,const char *a2,const char *a3,const char *a4,const char *a5,a1,a2,a3,a4,a5)
  
    OutputList(const OutputList &ol);
    QList<OutputGenerator> *outputs;
};

