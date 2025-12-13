import digital.vasic.yole.format.jupyter.JupyterParser
import digital.vasic.yole.format.TextFormat

fun main() {
    val parser = JupyterParser()
    
    // Test different kernel variants
    val python2Notebook = """
        {
            "cells": [
                {
                    "cell_type": "code",
                    "execution_count": 1,
                    "metadata": {},
                    "outputs": [],
                    "source": ["print 'Hello from Python 2'"]
                }
            ],
            "metadata": {
                "kernelspec": {
                    "display_name": "Python 2",
                    "language": "python",
                    "name": "python2"
                }
            },
            "nbformat": 4,
            "nbformat_minor": 2
        }
    """.trimIndent()
    
    val rNotebook = """
        {
            "cells": [
                {
                    "cell_type": "code",
                    "execution_count": 1,
                    "metadata": {},
                    "outputs": [],
                    "source": ["print("Hello from R!")"]
                }
            ],
            "metadata": {
                "kernelspec": {
                    "display_name": "R",
                    "language": "R",
                    "name": "ir"
                }
            },
            "nbformat": 4,
            "nbformat_minor": 2
        }
    """.trimIndent()
    
    try {
        // Test Python 2 variant
        val python2Result = parser.parse(python2Notebook)
        println("✅ Python 2 kernel test passed!")
        println("Format: ${python2Result.format.id}")
        println("Kernel: ${python2Result.metadata["kernel"]}")
        println("Language: ${python2Result.metadata["language"]}")
        println("HTML contains expected elements: ${python2Result.parsedContent.contains("Kernel: python2")}")
        
        // Test R variant
        val rResult = parser.parse(rNotebook)
        println("\n✅ R kernel test passed!")
        println("Format: ${rResult.format.id}")
        println("Kernel: ${rResult.metadata["kernel"]}")
        println("Language: ${rResult.metadata["language"]}")
        println("HTML contains expected elements: ${rResult.parsedContent.contains("Kernel: ir")}")
        
        println("\n🎉 All Jupyter variant tests passed!")
        
    } catch (e: Exception) {
        println("❌ Jupyter variant test failed: ${e.message}")
        e.printStackTrace()
    }
}