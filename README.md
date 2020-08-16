# Track Schematic
A program which turns csv files into track svg files, intended to be used with https://github.com/wtfstantheman/Network-Rail-ActiveMQ but can be used anywhere. This version uses render engine 1.5.
# Syntax

Command | Arguments | Example | Notes
--- | --- | --- | ---
text | text=, style=, note= | text?text=To/From&style=font-style:italic;font-size:14px;&note=true | Style attribute is added to the css of the element. Note is set to true when you want the text to showup in the jump menu
track | None | track | None
signal | face=[backwards/forwards], id=, display= | signal?face=backwards&id=BXS4783&display=S4783 | Display is whats shown, while id is the elemts id
berth | id= | berth?id=BX4783 | None
points | turnout=[left/right], face=[forwards/backwards] | points?turnout=right&face=forwards | None
half | turnout=[left/right], face=[forwards/backwards] | half?face=backwards&turnout=left | None
across | turnout=[left/right], face=[forwards/backwards] | across?face=forwards&turnout=left | None
platform | position=[top/bottom], text= | platform?position=bottom&text=1 | None

Multiple elements can be combined by using [] containing elements seporated by commas, for example

```
[berth?id=BX4739, platform?position=bottom&text=1]
```
