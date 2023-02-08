# -*- coding: utf-8 -*-
#
# OSMOSE documentation build configuration file, created by
# sphinx-quickstart on Tue Aug  8 09:44:06 2017.
#
# This file is execfile()d with the current directory set to its
# containing dir.
#
# Note that not all possible configuration values are present in this
# autogenerated file.
#
# All configuration values have a default; values that are commented out
# serve to show the default.

# If extensions (or modules to document with autodoc) are in another directory,
# add these directories to sys.path here. If the directory is relative to the
# documentation root, use os.path.abspath to make it absolute, like shown here.
#
# import os
# import sys
# sys.path.insert(0, os.path.abspath('.'))
import os
import re
from datetime import date
import sphinx_rtd_theme

# -- General configuration ------------------------------------------------

# If your documentation needs a minimal Sphinx version, state it here.
#
# needs_sphinx = '1.0'

# Add any Sphinx extension module names here, as strings. They can be
# extensions coming with Sphinx (named 'sphinx.ext.*') or your custom
# ones.
extensions = ['sphinx.ext.todo',
    'sphinx.ext.mathjax',
    'sphinx.ext.intersphinx',
    'sphinx.ext.githubpages',
    'sphinxcontrib.bibtex',
    'sphinxcontrib.programoutput',
    'IPython.sphinxext.ipython_directive',
    'IPython.sphinxext.ipython_console_highlighting',
    'matplotlib.sphinxext.plot_directive',
    'sphinxcontrib.mermaid',
    'sphinx_rtd_theme',
]

plantuml = 'plantuml'
plantuml_output_format = 'svg_img'
plantuml_latex_output_format = 'pdf'

mermaid_pdfcrop = 'pdfcrop'
#mermaid_output_format = 'png'

bibtex_bibfiles = ['_static/biblio.bib']
bibtex_reference_style = 'author_year'

# Add any paths that contain templates here, relative to this directory.
templates_path = ['_templates']

# The suffix(es) of source filenames.
# You can specify multiple suffix as a list of string:
#
# source_suffix = ['.rst', '.md']
source_suffix = '.rst'

# The master toctree document.
master_doc = 'index'

# General information about the project.
project = u'OSMOSE'
author = u'Nicolas Barrier, Yunne-Jai Shin, Philippe Verley, Morgane Travers, Laure Velez, Ricardo Oliveros-Ramos, Arnaud Grüss, Alaia Morell, Hanna Schenk'

copyright = '%s, %s' %(date.today().strftime("%Y-%m-%d"), author)

# The version info for the project you're documenting, acts as replacement for
# |version| and |release|, also used in various other places throughout the
# built documents.
#
# Recover the Ichthyop version based
pom_file = os.path.join('..', 'pom.xml')
with open(pom_file, 'r') as fpom:
    lines = fpom.readlines()
    regex = re.compile(' *\<version\>(.*)\</version\>')
    for l in lines:
        if regex.match(l):
            version = regex.match(l).groups()[0]
            break

# include to to references
todo_include_todos = True
todo_emit_warnings = True

# The language for content autogenerated by Sphinx. Refer to documentation
# for a list of supported languages.
#
# This is also used if you do content translation via gettext catalogs.
# Usually you set "language" from the command line for these cases.
#language = "en"

# List of patterns, relative to source directory, that match files and
# directories to ignore when looking for source files.
# This patterns also effect to html_static_path and html_extra_path
exclude_patterns = ['_build', 'Thumbs.db', '.DS_Store', 'alias.rst', 'index_private.rst', 'index_public.rst']

# The name of the Pygments (syntax highlighting) style to use.
pygments_style = 'sphinx'

# If true, `todo` and `todoList` produce output, else they produce nothing.
todo_include_todos = True

# Included at the beginning of every source file that is read.
with open('alias.rst', 'r') as pr:
    rst_prolog = pr.read()


# -- Options for HTML output ----------------------------------------------

# The theme to use for HTML and HTML Help pages.  See the documentation for
# a list of builtin themes.

# use figure numbers for referencing figures
numfig = True
# use sections as a reference for figures: X.1, X.2 with X the section
numfig_secnum_depth = (1)

html_theme = "sphinx_rtd_theme"

# Theme options are theme-specific and customize the look and feel of a theme
# further.  For a list of options available for each theme, see the
# documentation.
#
# html_theme_options = {}

# Add any paths that contain custom static files (such as style sheets) here,
# relative to this directory. They are copied after the builtin static files,
# so a file named "default.css" will overwrite the builtin "default.css".
html_static_path = ['_static']
html_css_files = ['css/hacks.css']

#html_context = {
#        'css_files': [
#            '_static/theme_overrides.css',  # override wide tables in RTD theme
#            ],
#        }

def setup(app):
   app.add_css_file('theme_overrides.css')

# -- Options for HTMLHelp output ------------------------------------------

# Output file base name for HTML help builder.
htmlhelp_basename = 'OSMOSEdoc'


# -- Options for LaTeX output ---------------------------------------------

latex_elements = {
    # The paper size ('letterpaper' or 'a4paper').
    #
    # 'papersize': 'letterpaper',

    # The font size ('10pt', '11pt' or '12pt').
    #
    # 'pointsize': '10pt',

    # Additional stuff for the LaTeX preamble.
    #
    # 'preamble': '',

    # Latex figure (float) alignment
    #
    # 'figure_align': 'htbp',
}

# Grouping the document tree into LaTeX files. List of tuples
# (source start file, target name, title,
#  author, documentclass [howto, manual, or own class]).
latexauthor = '\\and'.join(author.split(','))

latex_documents = [
    (master_doc, 'OSMOSE.tex', u'OSMOSE Documentation', latexauthor, 'manual'),
]


# -- Options for manual page output ---------------------------------------

# One entry per manual page. List of tuples
# (source start file, name, description, authors, manual section).
man_pages = [
    (master_doc, 'osmose', u'OSMOSE Documentation',
     [author], 1)
]


# -- Options for Texinfo output -------------------------------------------

# Grouping the document tree into Texinfo files. List of tuples
# (source start file, target name, title, author,
#  dir menu entry, description, category)
texinfo_documents = [
    (master_doc, 'OSMOSE', u'OSMOSE Documentation',
     author, 'OSMOSE', 'One line description of project.',
     'Miscellaneous'),
]
