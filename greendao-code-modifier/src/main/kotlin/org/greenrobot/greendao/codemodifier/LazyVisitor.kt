package org.greenrobot.greendao.codemodifier

import org.greenrobot.eclipse.jdt.core.dom.*

/** ASTVisitor that visits nothing by default */
open class LazyVisitor : ASTVisitor() {
    override fun visit(node: AnnotationTypeDeclaration): Boolean  = false

    override fun visit(node: AnnotationTypeMemberDeclaration): Boolean  = false

    override fun visit(node: AnonymousClassDeclaration): Boolean  = false

    override fun visit(node: ArrayAccess): Boolean  = false

    override fun visit(node: ArrayCreation): Boolean  = false

    override fun visit(node: ArrayInitializer): Boolean  = false

    override fun visit(node: ArrayType): Boolean  = false

    override fun visit(node: AssertStatement): Boolean  = false

    override fun visit(node: Assignment): Boolean  = false

    override fun visit(node: Block): Boolean  = false

    override fun visit(node: BlockComment): Boolean  = false

    override fun visit(node: BooleanLiteral): Boolean  = false

    override fun visit(node: BreakStatement): Boolean  = false

    override fun visit(node: CastExpression): Boolean  = false

    override fun visit(node: CatchClause): Boolean  = false

    override fun visit(node: CharacterLiteral): Boolean  = false

    override fun visit(node: ClassInstanceCreation): Boolean  = false

    override fun visit(node: CompilationUnit): Boolean  = false

    override fun visit(node: ConditionalExpression): Boolean  = false

    override fun visit(node: ConstructorInvocation): Boolean  = false

    override fun visit(node: ContinueStatement): Boolean  = false

    override fun visit(node: CreationReference): Boolean  = false

    override fun visit(node: Dimension): Boolean  = false

    override fun visit(node: DoStatement): Boolean  = false

    override fun visit(node: EmptyStatement): Boolean  = false

    override fun visit(node: EnhancedForStatement): Boolean  = false

    override fun visit(node: EnumConstantDeclaration): Boolean  = false

    override fun visit(node: EnumDeclaration): Boolean  = false

    override fun visit(node: ExpressionMethodReference): Boolean  = false

    override fun visit(node: ExpressionStatement): Boolean  = false

    override fun visit(node: FieldAccess): Boolean  = false

    override fun visit(node: FieldDeclaration): Boolean  = false

    override fun visit(node: ForStatement): Boolean  = false

    override fun visit(node: IfStatement): Boolean  = false

    override fun visit(node: ImportDeclaration): Boolean  = false

    override fun visit(node: InfixExpression): Boolean  = false

    override fun visit(node: Initializer): Boolean  = false

    override fun visit(node: InstanceofExpression): Boolean  = false

    override fun visit(node: IntersectionType): Boolean  = false

    override fun visit(node: Javadoc): Boolean  = false

    override fun visit(node: LabeledStatement): Boolean  = false

    override fun visit(node: LambdaExpression): Boolean  = false

    override fun visit(node: LineComment): Boolean  = false

    override fun visit(node: MarkerAnnotation): Boolean  = false

    override fun visit(node: MemberRef): Boolean  = false

    override fun visit(node: MemberValuePair): Boolean  = false

    override fun visit(node: MethodRef): Boolean  = false

    override fun visit(node: MethodRefParameter): Boolean  = false

    override fun visit(node: MethodDeclaration): Boolean  = false

    override fun visit(node: MethodInvocation): Boolean  = false

    override fun visit(node: Modifier): Boolean  = false

    override fun visit(node: NameQualifiedType): Boolean  = false

    override fun visit(node: NormalAnnotation): Boolean  = false

    override fun visit(node: NullLiteral): Boolean  = false

    override fun visit(node: NumberLiteral): Boolean  = false

    override fun visit(node: PackageDeclaration): Boolean  = false

    override fun visit(node: ParameterizedType): Boolean  = false

    override fun visit(node: ParenthesizedExpression): Boolean  = false

    override fun visit(node: PostfixExpression): Boolean  = false

    override fun visit(node: PrefixExpression): Boolean  = false

    override fun visit(node: PrimitiveType): Boolean  = false

    override fun visit(node: QualifiedName): Boolean  = false

    override fun visit(node: QualifiedType): Boolean  = false

    override fun visit(node: ReturnStatement): Boolean  = false

    override fun visit(node: SimpleName): Boolean  = false

    override fun visit(node: SimpleType): Boolean  = false

    override fun visit(node: SingleMemberAnnotation): Boolean  = false

    override fun visit(node: SingleVariableDeclaration): Boolean  = false

    override fun visit(node: StringLiteral): Boolean  = false

    override fun visit(node: SuperConstructorInvocation): Boolean  = false

    override fun visit(node: SuperFieldAccess): Boolean  = false

    override fun visit(node: SuperMethodInvocation): Boolean  = false

    override fun visit(node: SuperMethodReference): Boolean  = false

    override fun visit(node: SwitchCase): Boolean  = false

    override fun visit(node: SwitchStatement): Boolean  = false

    override fun visit(node: SynchronizedStatement): Boolean  = false

    override fun visit(node: TagElement): Boolean  = false

    override fun visit(node: TextElement): Boolean  = false

    override fun visit(node: ThisExpression): Boolean  = false

    override fun visit(node: ThrowStatement): Boolean  = false

    override fun visit(node: TryStatement): Boolean  = false

    override fun visit(node: TypeDeclaration): Boolean  = false

    override fun visit(node: TypeDeclarationStatement): Boolean  = false

    override fun visit(node: TypeLiteral): Boolean  = false

    override fun visit(node: TypeMethodReference): Boolean  = false

    override fun visit(node: TypeParameter): Boolean  = false

    override fun visit(node: UnionType): Boolean  = false

    override fun visit(node: VariableDeclarationExpression): Boolean  = false

    override fun visit(node: VariableDeclarationStatement): Boolean  = false

    override fun visit(node: VariableDeclarationFragment): Boolean  = false

    override fun visit(node: WhileStatement): Boolean  = false

    override fun visit(node: WildcardType): Boolean  = false
}