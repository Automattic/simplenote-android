Engineering Notes

7 September, 2014

INTRO
This file contains some engineering notes that can be referenced during design.  It should be
deleted before merging into the develop branch.

TODO: Delete this file before merging into develop


Richard Schilling   7 SEP 2014

    * There are two widgets in the app:

        button widget: a widget with buttons
            (widget image: app icon, @drawable/ic_launcher)

        list widget: a widget that shows notes in a list
            (widget image:  drawer icon, @drawable/ic_drawer).

    * The widget images presented in the OS widget menu needs to be designed.
    * The user interface elements for the widgets themselves need design/design review.
    * String resources need to be finalized.
    * New resources (e.g. dimensions/strings/etc) need to be checked/created.
    * Should a configuration activity be used? See ConfigureWidget.java.
    * Some of the known issues:

        Sometimes when a new note is added, the title of the note doesn't sow up in the widget.
        It's almost as if the cursor doesn't get populated

        Sizing handles around the button widget seem too far away from the widget itself.
        Those should fit snug against the widget.  To see this put the button widget on the
        home screen and then try to resize it.


    * networking use cases were not considered.  App needs testing
    * A loading user interface needs to be designed.  Can we incorporate the "beep ... boop ... beep"
            hilarity used on the Wordpress site?  I love that feature.

    * SImpleNotesWidgetProvider and ListWidgetProvider: it's irritating to have to constantly
        extract the extras from the intent and do string comparisons.  So, I use a simple
        command pattern to execute.

    * Additional actions can be added/removed from the widget providers by updating the command map.
        Once this bit of code is factored into a base class it will reduce some duplicate lines
        (ex: in onReceive for example).

    TODO: refactor widget provider class names
    TODO: refactor widget class structure to consolidate common code.
    TODO: explore - use many intent actions in SimpleNoteWidgetProvider or use one action with extras?
    TODO: javadoc
    TODO: remove logging





