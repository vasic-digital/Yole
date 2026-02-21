make sure that we always run all tests and perform all build processes from the Compose created container (Docker or Podman) which    
  will bring the proper environment with all dependencies required to perform / execute all tests, write detailed reports and execute   
  build of all application / components / services. make sure that all integration services (all existing containers) are up and
  running and accessible from the container for testing and building and that proper directories of the project are bound so we have    
  as the end result all applications created in releases directory - all the latest versions. make sure that all signing keys are     
  available (for signing android and other apps) with some defaults so we can have signed artifcats (apps) as the result of the build
  process which can be installed and tested on real devices. all integration, full automation tests must be executed on proper android
  emulator. make sure we bring proper android emulator which will run and install all android applications. same applies for desktop
  testing, web and others. user Roboelectric framework for full automation Android tests and proper frameworks equivalents for all
  other apps / services (platforms we have). coverage must be no less than 100% with success rate of 100%. proper validation and
  verification for false positives (false success) must be performed! document everything!
