package com.intellij.psi.impl.source.xml;

import com.intellij.ide.util.EditSourceUtil;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.impl.meta.MetaRegistry;
import com.intellij.psi.meta.PsiMetaData;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.ChildRoleBase;
import com.intellij.psi.xml.*;
import com.intellij.util.IncorrectOperationException;
import com.intellij.xml.util.XmlUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

/**
 * @author Mike
 */
public class XmlAttributeDeclImpl extends XmlElementImpl implements XmlAttributeDecl, XmlElementType {
  private static final Logger LOG = Logger.getInstance("#com.intellij.psi.impl.source.xml.XmlAttributeDeclImpl");
  @NonNls private static final String ID_ATT = "ID";
  @NonNls private static final String IDREF_ATT = "IDREF";

  public XmlAttributeDeclImpl() {
    super(XML_ATTRIBUTE_DECL);
  }

  public int getChildRole(ASTNode child) {
    LOG.assertTrue(child.getTreeParent() == this);
    IElementType i = child.getElementType();
    if (i == XML_NAME) {
      return XmlChildRole.XML_NAME;
    }
    else if (i == XML_ATT_REQUIRED) {
      return XmlChildRole.XML_ATT_REQUIRED;
    }
    else if (i == XML_ATT_FIXED) {
      return XmlChildRole.XML_ATT_FIXED;
    }
    else if (i == XML_ATT_IMPLIED) {
      return XmlChildRole.XML_ATT_IMPLIED;
    }
    else if (i == XML_ATTRIBUTE_VALUE) {
      return XmlChildRole.XML_DEFAULT_VALUE;
    }
    else if (i == XML_ENUMERATED_TYPE) {
      return XmlChildRole.XML_ENUMERATED_TYPE;
    }
    else {
      return ChildRoleBase.NONE;
    }
  }

  public XmlElement getNameElement() {
    return findElementByTokenType(XML_NAME);
  }

  public boolean isAttributeRequired() {
    return findElementByTokenType(XML_ATT_REQUIRED) != null;
  }

  public boolean isAttributeFixed() {
    return findElementByTokenType(XML_ATT_FIXED) != null;
  }

  public boolean isAttributeImplied() {
    return findElementByTokenType(XML_ATT_IMPLIED) != null;
  }

  public XmlAttributeValue getDefaultValue() {
    return (XmlAttributeValue)findElementByTokenType(XML_ATTRIBUTE_VALUE);
  }

  public String getDefaultValueText() {
    XmlAttributeValue value = getDefaultValue();
    if (value == null) return null;
    String text = value.getText();
    if (text.indexOf('%') == -1 && text.indexOf('&') == -1) return text;

    final StringBuilder builder = new StringBuilder();
    value.processElements(new PsiElementProcessor() {
      public boolean execute(PsiElement element) {
        builder.append(element.getText());
        return true;
      }
    }, null);
    return builder.toString();
  }

  public boolean isEnumerated() {
    return findElementByTokenType(XML_ENUMERATED_TYPE) != null;
  }

  public XmlElement[] getEnumeratedValues() {
    XmlEnumeratedType enumeratedType = (XmlEnumeratedType)findElementByTokenType(XML_ENUMERATED_TYPE);
    if (enumeratedType != null) {
      return enumeratedType.getEnumeratedValues();
    }
    else {
      return XmlElement.EMPTY_ARRAY;
    }
  }

  public boolean isIdAttribute() {
    final PsiElement elementType = findElementType();

    return elementType != null && elementType.getText().equals(ID_ATT);
  }

  private PsiElement findElementType() {
    final PsiElement elementName = findElementByTokenType(XML_NAME);
    final PsiElement nextSibling = (elementName != null) ? elementName.getNextSibling() : null;
    final PsiElement elementType = (nextSibling instanceof PsiWhiteSpace) ? nextSibling.getNextSibling() : nextSibling;

    return elementType;
  }

  public boolean isIdRefAttribute() {
    final PsiElement elementType = findElementType();

    return elementType != null && elementType.getText().equals(IDREF_ATT);
  }

  public PsiMetaData getMetaData() {
    return MetaRegistry.getMeta(this);
  }

  public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
    XmlElementChangeUtil.doNameReplacement(this, getNameElement(), name);
    return null;
  }

  public String getName() {
    XmlElement name = getNameElement();
    return (name != null) ? name.getText() : null;
  }

  public boolean canNavigate() {
    if (isPhysical()) return super.canNavigate();
    final PsiNamedElement psiNamedElement = XmlUtil.findRealNamedElement(this);
    return psiNamedElement != null && psiNamedElement != this && ((Navigatable)psiNamedElement).canNavigate();
  }

  public void navigate(final boolean requestFocus) {
    if (isPhysical()) {
      super.navigate(requestFocus);
      return;
    }
    final PsiNamedElement psiNamedElement = XmlUtil.findRealNamedElement(this);
    Navigatable navigatable = EditSourceUtil.getDescriptor(psiNamedElement);

    if (psiNamedElement instanceof XmlEntityDecl) {
      final OpenFileDescriptor fileDescriptor = (OpenFileDescriptor)navigatable;
      navigatable = new OpenFileDescriptor(
        fileDescriptor.getProject(),
        fileDescriptor.getFile(),
        psiNamedElement.getTextRange().getStartOffset() + psiNamedElement.getText().indexOf(getName())
      );
    }
    navigatable.navigate(requestFocus);
  }

  @NotNull
  public PsiElement getNavigationElement() {
    return this;
  }
}
