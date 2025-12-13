import digital.vasic.yole.format.jupyter.JupyterParser
import digital.vasic.yole.format.TextFormat

fun main() {
    val parser = JupyterParser()
    
    val simpleNotebook = """
        {
            "cells": [
                {
                    "cell_type": "code",
                    "execution_count": 1,
                    "metadata": {},
                    "outputs": [],
                    "source": ["print('Hello, World!')"]
                }
            ],
            "metadata": {
                "kernelspec": {
                    "name": "python3"
                }
            },
            "nbformat": 4,
            "nbformat_minor": 2
        }
    """.trimIndent()
    
    try {
        val result = parser.parse(simpleNotebook)
        println("✅ Jupyter parser test passed!")
        println("Format: ${result.format.id}")
        println("Cells: ${result.metadata["cells"]}")
        println("Kernel: ${result.metadata["kernel"]}")
        println("HTML contains expected elements: ${result.parsedContent.contains("Hello, World!")}")
    } catch (e: Exception) {
        println("❌ Jupyter parser test failed: ${e.message}")
        e.printStackTrace()
    }
}