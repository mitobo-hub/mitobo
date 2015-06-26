PDFFILE =	MiToBoGuide.pdf
EXPORTPDFFILE =	MiToBoGuide-1.0.pdf

TEXFILES = 	MiToBoGuide.tex			\
			defines.tex						\
			contents.tex					\
			Welcome.tex						\
			UsingMitobo.tex				\
			OpRunnerImageJ.tex		\
			OpRunnerCmdline.tex		\
			HistoryGraphs.tex	\
			Grappa.tex						\
			Configuration.tex			\
			ProgrammingMitobo.tex	\
			Implementation.tex		\
			ImplPlugins.tex		\
			Datatypes.tex		\
			Tools.tex
			
IMAGES = 	../images/ScreenshotGrappa.png					\
			../images/ScreenshotImageJ_trans.png		\
			../images/ScreenshotOpRunner.png				\
			../images/ScreenshotSnakeOp_trans.png

all: $(PDFFILE)

$(PDFFILE):	$(TEXFILES) $(IMAGES)
	pdflatex MiToBoGuide.tex
	bibtex MiToBoGuide
	pdflatex MiToBoGuide.tex
	pdflatex MiToBoGuide.tex

install: $(PDFFILE)
	cp $(PDFFILE) ../../../../exportonrelease/$(EXPORTPDFFILE)
